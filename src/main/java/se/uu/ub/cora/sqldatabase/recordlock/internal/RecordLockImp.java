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

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.recordlock.RecordLock;
import se.uu.ub.cora.sqldatabase.recordlock.RecordLockException;

public class RecordLockImp implements RecordLock {
	private static final String INSERT_SQL = "insert into recordlock (type, id) values (?, ?);";
	private static final String DELETE_SQL = "delete from recordlock where type = ? and id = ?;";
	private static final long RETRY_INTERVAL_MILLIS = 50;
	private static final long MAX_WAIT_MILLIS = 60_000;

	private DatabaseFacade databaseFacade;
	private Sleeper sleeper;
	private TimeSource timeSource;

	public static RecordLockImp usingDatabaseFacade(DatabaseFacade databaseFacade) {
		return new RecordLockImp(databaseFacade, new ThreadSleeper(), new SystemTimeSource());
	}

	static RecordLockImp usingDatabaseFacadeAndSleeperAndTimeSource(DatabaseFacade databaseFacade,
			Sleeper sleeper, TimeSource timeSource) {
		return new RecordLockImp(databaseFacade, sleeper, timeSource);
	}

	private RecordLockImp(DatabaseFacade databaseFacade, Sleeper sleeper, TimeSource timeSource) {
		this.databaseFacade = databaseFacade;
		this.sleeper = sleeper;
		this.timeSource = timeSource;
	}

	@Override
	public void acquire(String type, String id) {
		long deadline = timeSource.currentTimeMillis() + MAX_WAIT_MILLIS;
		while (!tryToInsertLock(type, id, deadline)) {
			sleeper.sleepMilliseconds(RETRY_INTERVAL_MILLIS);
		}
	}

	private boolean tryToInsertLock(String type, String id, long deadline) {
		try {
			insertLock(type, id);
			return true;
		} catch (SqlConflictException e) {
			throwIfTimeLimitReached(deadline, type, id, e);
			return false;
		}
	}

	private void insertLock(String type, String id) {
		databaseFacade.executeSqlWithValues(INSERT_SQL, List.of(type, id));
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

	@Override
	public void release(String type, String id) {
		databaseFacade.executeSqlWithValues(DELETE_SQL, List.of(type, id));
	}

	@Override
	public void close() {
		databaseFacade.close();
	}

	public DatabaseFacade onlyForTestGetDatabaseFacade() {
		return databaseFacade;
	}

	public Sleeper onlyForTestGetSleeper() {
		return sleeper;
	}

	public TimeSource onlyForTestGetTimeSource() {
		return timeSource;
	}
}
