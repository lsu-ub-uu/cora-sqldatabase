/*
 * Copyright 2026 Uppsala University Library
 *
 * This file is part of Cora.
 *
 *     Cora is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 *
 *     Cora is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 *
 *     You should have received a copy of the GNU General Public License
 *     along with Cora.  If not, see <http://www.gnu.org/licenses/>.
 */
package se.uu.ub.cora.sqldatabase.recordlock.internal;

import java.util.List;
import java.util.function.Consumer;

import se.uu.ub.cora.recordlock.RecordLock;
import se.uu.ub.cora.recordlock.RecordLockException;
import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseFactory;

/**
 * RecordLockImp implements {@link RecordLock} using a lock row in the recordlock table, which has
 * (type, id) as primary key.
 * <p>
 * Each call to acquire or release uses its own, new {@link DatabaseFacade} that is closed before
 * the call returns. This ensures that a lock operation never runs inside a transaction started by
 * someone else, and that no database resources are kept between calls.
 */
public class RecordLockImp implements RecordLock {
	private static final String INSERT_SQL = "insert into recordlock (type, id) values (?, ?);";
	private static final String DELETE_SQL = "delete from recordlock where type = ? and id = ?;";
	private static final long RETRY_INTERVAL_MILLIS = 50;
	private static final long MAX_WAIT_MILLIS = 60_000;

	private SqlDatabaseFactory sqlDatabaseFactory;
	private Sleeper sleeper;
	private TimeSource timeSource;

	public static RecordLockImp usingSqlDatabaseFactory(SqlDatabaseFactory sqlDatabaseFactory) {
		return new RecordLockImp(sqlDatabaseFactory, new ThreadSleeper(), new SystemTimeSource());
	}

	static RecordLockImp usingSqlDatabaseFactoryAndSleeperAndTimeSource(
			SqlDatabaseFactory sqlDatabaseFactory, Sleeper sleeper, TimeSource timeSource) {
		return new RecordLockImp(sqlDatabaseFactory, sleeper, timeSource);
	}

	private RecordLockImp(SqlDatabaseFactory sqlDatabaseFactory, Sleeper sleeper,
			TimeSource timeSource) {
		this.sqlDatabaseFactory = sqlDatabaseFactory;
		this.sleeper = sleeper;
		this.timeSource = timeSource;
	}

	@Override
	public void acquire(String type, String id) {
		useNewDatabaseFacade("acquire", type, id,
				databaseFacade -> insertLockWaitingForOtherHolder(databaseFacade, type, id));
	}

	@Override
	public void release(String type, String id) {
		useNewDatabaseFacade("release", type, id,
				databaseFacade -> executeWithTypeAndId(databaseFacade, DELETE_SQL, type, id));
	}

	private void useNewDatabaseFacade(String action, String type, String id,
			Consumer<DatabaseFacade> operation) {
		try (DatabaseFacade databaseFacade = sqlDatabaseFactory.factorDatabaseFacade()) {
			operation.accept(databaseFacade);
		} catch (RecordLockException e) {
			throw e;
		} catch (RuntimeException e) {
			throw RecordLockException.withMessageAndException(
					"Could not " + action + " lock for type: " + type + " and id: " + id + ".", e);
		}
	}

	private void insertLockWaitingForOtherHolder(DatabaseFacade databaseFacade, String type,
			String id) {
		long deadline = timeSource.currentTimeMillis() + MAX_WAIT_MILLIS;
		while (!tryToInsertLock(databaseFacade, type, id, deadline)) {
			sleeper.sleepMilliseconds(RETRY_INTERVAL_MILLIS);
		}
	}

	private boolean tryToInsertLock(DatabaseFacade databaseFacade, String type, String id,
			long deadline) {
		try {
			executeWithTypeAndId(databaseFacade, INSERT_SQL, type, id);
			return true;
		} catch (SqlConflictException e) {
			throwIfTimeLimitReached(deadline, type, id, e);
			return false;
		}
	}

	private void executeWithTypeAndId(DatabaseFacade databaseFacade, String sql, String type,
			String id) {
		databaseFacade.executeSqlWithValues(sql, List.of(type, id));
	}

	private void throwIfTimeLimitReached(long deadline, String type, String id,
			SqlConflictException conflict) {
		if (timeSource.currentTimeMillis() >= deadline) {
			throw RecordLockException.withMessageAndException(
					"Could not acquire lock for type: " + type + " and id: " + id + " within "
							+ MAX_WAIT_MILLIS + " ms.",
					conflict);
		}
	}

	public SqlDatabaseFactory onlyForTestGetSqlDatabaseFactory() {
		return sqlDatabaseFactory;
	}

	public Sleeper onlyForTestGetSleeper() {
		return sleeper;
	}

	public TimeSource onlyForTestGetTimeSource() {
		return timeSource;
	}
}
