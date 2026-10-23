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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.recordlock.RecordLock;
import se.uu.ub.cora.recordlock.RecordLockException;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.sequence.DatabaseFacadeSpy;

public class RecordLockTest {
	private static final String INSERT_SQL = "insert into recordlock (type, id) values (?, ?);";
	private static final String DELETE_SQL = "delete from recordlock where type = ? and id = ?;";
	private static final String SOME_TYPE = "someType";
	private static final String SOME_ID = "someId";
	private static final long RETRY_INTERVAL_MILLIS = 50;
	private static final long MAX_WAIT_MILLIS = 60_000;
	private static final String ACQUIRE_ERROR = "Could not acquire lock for type: someType and id: "
			+ "someId.";
	private static final String RELEASE_ERROR = "Could not release lock for type: someType and id: "
			+ "someId.";

	private SqlDatabaseFactorySpy sqlDatabaseFactory;
	private DatabaseFacadeSpy databaseFacade;
	private SleeperSpy sleeper;
	private TimeSourceSpy timeSource;
	private RecordLock recordLock;

	@BeforeMethod
	private void beforeMethod() {
		sqlDatabaseFactory = new SqlDatabaseFactorySpy();
		databaseFacade = new DatabaseFacadeSpy();
		sqlDatabaseFactory.MRV.setDefaultReturnValuesSupplier("factorDatabaseFacade",
				() -> databaseFacade);
		sleeper = new SleeperSpy();
		timeSource = new TimeSourceSpy();
		recordLock = RecordLockImp.usingSqlDatabaseFactoryAndSleeperAndTimeSource(
				sqlDatabaseFactory, sleeper, timeSource);
	}

	@Test
	public void testUsingSqlDatabaseFactoryUsesRealSleeperAndTimeSource() {
		RecordLockImp recordLockImp = RecordLockImp.usingSqlDatabaseFactory(sqlDatabaseFactory);

		assertSame(recordLockImp.onlyForTestGetSqlDatabaseFactory(), sqlDatabaseFactory);
		assertTrue(recordLockImp.onlyForTestGetSleeper() instanceof ThreadSleeper);
		assertTrue(recordLockImp.onlyForTestGetTimeSource() instanceof SystemTimeSource);
	}

	@Test
	public void testOnlyForTestGetters() {
		RecordLockImp recordLockImp = (RecordLockImp) recordLock;

		assertSame(recordLockImp.onlyForTestGetSqlDatabaseFactory(), sqlDatabaseFactory);
		assertSame(recordLockImp.onlyForTestGetSleeper(), sleeper);
		assertSame(recordLockImp.onlyForTestGetTimeSource(), timeSource);
	}

	@Test
	public void testAcquireInsertsLockUsingNewDatabaseFacadeAndClosesIt() {
		recordLock.acquire(SOME_TYPE, SOME_ID);

		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		assertExecutedSqlWithTypeAndId(databaseFacade, 0, INSERT_SQL);
		databaseFacade.MCR.assertMethodNotCalled("readUsingSqlAndValues");
		databaseFacade.MCR.assertMethodNotCalled("readOneRowOrFailUsingSqlAndValues");
		databaseFacade.MCR.assertMethodNotCalled("startTransaction");
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
		sleeper.MCR.assertMethodNotCalled("sleepMilliseconds");
	}

	@Test
	public void testEachOperationUsesItsOwnNewDatabaseFacade() {
		sqlDatabaseFactory.MRV.setDefaultReturnValuesSupplier("factorDatabaseFacade",
				DatabaseFacadeSpy::new);

		recordLock.acquire(SOME_TYPE, SOME_ID);
		recordLock.release(SOME_TYPE, SOME_ID);
		recordLock.acquire(SOME_TYPE, SOME_ID);

		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 3);
		assertFacadeExecutedOnceAndWasClosed(0, INSERT_SQL);
		assertFacadeExecutedOnceAndWasClosed(1, DELETE_SQL);
		assertFacadeExecutedOnceAndWasClosed(2, INSERT_SQL);
	}

	private void assertFacadeExecutedOnceAndWasClosed(int callNumber, String sql) {
		DatabaseFacadeSpy facade = (DatabaseFacadeSpy) sqlDatabaseFactory.MCR
				.getReturnValue("factorDatabaseFacade", callNumber);
		facade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		assertExecutedSqlWithTypeAndId(facade, 0, sql);
		facade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testAcquireRetriesEvery50msUsingSameDatabaseFacadeUntilLockIsInserted() {
		throwConflictForNumberOfInserts(3);

		recordLock.acquire(SOME_TYPE, SOME_ID);

		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 4);
		for (int call = 0; call < 4; call++) {
			assertExecutedSqlWithTypeAndId(databaseFacade, call, INSERT_SQL);
		}
		sleeper.MCR.assertNumberOfCallsToMethod("sleepMilliseconds", 3);
		for (int call = 0; call < 3; call++) {
			sleeper.MCR.assertParameters("sleepMilliseconds", call, RETRY_INTERVAL_MILLIS);
		}
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testAcquireReadsStartTimeOnceAndTimeAfterEachConflict() {
		throwConflictForNumberOfInserts(2);

		recordLock.acquire(SOME_TYPE, SOME_ID);

		timeSource.MCR.assertNumberOfCallsToMethod("currentTimeMillis", 3);
	}

	@Test
	public void testAcquireStillRetriesJustBeforeTimeLimit() {
		throwConflictForNumberOfInserts(1);
		timeSource.MRV.setReturnValues("currentTimeMillis",
				List.of(1_000L, 1_000L + MAX_WAIT_MILLIS - 1));

		recordLock.acquire(SOME_TYPE, SOME_ID);

		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 2);
		sleeper.MCR.assertNumberOfCallsToMethod("sleepMilliseconds", 1);
	}

	@Test
	public void testAcquireThrowsRecordLockExceptionWhenTimeLimitIsReached() {
		SqlConflictException conflict = setUpConflictUntilTimeLimitIsReached();

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertTimeLimitException(e, conflict);
			assertEquals(e.getSuppressed().length, 0);
		}
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 2);
		sleeper.MCR.assertNumberOfCallsToMethod("sleepMilliseconds", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	private SqlConflictException setUpConflictUntilTimeLimitIsReached() {
		SqlConflictException conflict = SqlConflictException.withMessage("duplicate key");
		databaseFacade.MRV.setAlwaysThrowException("executeSqlWithValues", conflict);
		timeSource.MRV.setReturnValues("currentTimeMillis",
				List.of(1_000L, 1_010L, 1_000L + MAX_WAIT_MILLIS));
		return conflict;
	}

	private void assertTimeLimitException(Exception e, SqlConflictException conflict) {
		assertTrue(e instanceof RecordLockException);
		assertEquals(e.getMessage(), "Could not acquire lock for type: " + SOME_TYPE
				+ " and id: " + SOME_ID + " within " + MAX_WAIT_MILLIS + " ms.");
		assertSame(e.getCause(), conflict);
	}

	@Test
	public void testAcquireTimeLimitWithFailingCloseKeepsCloseErrorAsSuppressed() {
		SqlConflictException conflict = setUpConflictUntilTimeLimitIsReached();
		SqlDatabaseException closeError = setUpCloseToFail();

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertTimeLimitException(e, conflict);
			assertSame(e.getSuppressed()[0], closeError);
		}
	}

	@Test
	public void testAcquireDoesNotRetryOnOtherDatabaseErrorsAndTranslatesThem() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("some error");
		databaseFacade.MRV.setAlwaysThrowException("executeSqlWithValues", error);

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, ACQUIRE_ERROR, error);
			assertEquals(e.getCause().getSuppressed().length, 0);
		}
		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		sleeper.MCR.assertMethodNotCalled("sleepMilliseconds");
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testAcquireFailingFactorDatabaseFacadeIsTranslatedAndNotRetried() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("no connection");
		sqlDatabaseFactory.MRV.setAlwaysThrowException("factorDatabaseFacade", error);

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, ACQUIRE_ERROR, error);
		}
		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
		sleeper.MCR.assertMethodNotCalled("sleepMilliseconds");
		timeSource.MCR.assertMethodNotCalled("currentTimeMillis");
	}

	@Test
	public void testAcquireFailingCloseAfterSuccessIsTranslated() {
		SqlDatabaseException closeError = setUpCloseToFail();

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, ACQUIRE_ERROR, closeError);
		}
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testAcquireFailingCloseAfterFailureIsKeptAsSuppressed() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("some error");
		databaseFacade.MRV.setAlwaysThrowException("executeSqlWithValues", error);
		SqlDatabaseException closeError = setUpCloseToFail();

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, ACQUIRE_ERROR, error);
			assertSame(e.getCause().getSuppressed()[0], closeError);
		}
	}

	@Test
	public void testAcquireInterruptedWhileWaitingIsPassedOnAndFacadeIsClosed() {
		throwConflictForNumberOfInserts(1);
		RecordLockException interrupted = RecordLockException.withMessage("interrupted");
		sleeper.MRV.setAlwaysThrowException("sleepMilliseconds", interrupted);

		try {
			recordLock.acquire(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertSame(e, interrupted);
		}
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testReleaseDeletesLockUsingNewDatabaseFacadeAndClosesIt() {
		recordLock.release(SOME_TYPE, SOME_ID);

		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		assertExecutedSqlWithTypeAndId(databaseFacade, 0, DELETE_SQL);
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
	}

	@Test
	public void testReleaseErrorIsTranslatedAndNotRetried() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("some error");
		databaseFacade.MRV.setAlwaysThrowException("executeSqlWithValues", error);

		try {
			recordLock.release(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, RELEASE_ERROR, error);
		}
		databaseFacade.MCR.assertNumberOfCallsToMethod("executeSqlWithValues", 1);
		databaseFacade.MCR.assertNumberOfCallsToMethod("close", 1);
		sleeper.MCR.assertMethodNotCalled("sleepMilliseconds");
	}

	@Test
	public void testReleaseFailingFactorDatabaseFacadeIsTranslated() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("no connection");
		sqlDatabaseFactory.MRV.setAlwaysThrowException("factorDatabaseFacade", error);

		try {
			recordLock.release(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, RELEASE_ERROR, error);
		}
		sqlDatabaseFactory.MCR.assertNumberOfCallsToMethod("factorDatabaseFacade", 1);
	}

	@Test
	public void testReleaseFailingCloseAfterSuccessIsTranslated() {
		SqlDatabaseException closeError = setUpCloseToFail();

		try {
			recordLock.release(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, RELEASE_ERROR, closeError);
		}
	}

	@Test
	public void testReleaseFailingCloseAfterFailureIsKeptAsSuppressed() {
		SqlDatabaseException error = SqlDatabaseException.withMessage("some error");
		databaseFacade.MRV.setAlwaysThrowException("executeSqlWithValues", error);
		SqlDatabaseException closeError = setUpCloseToFail();

		try {
			recordLock.release(SOME_TYPE, SOME_ID);
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertRecordLockException(e, RELEASE_ERROR, error);
			assertSame(e.getCause().getSuppressed()[0], closeError);
		}
	}

	private SqlDatabaseException setUpCloseToFail() {
		SqlDatabaseException closeError = SqlDatabaseException.withMessage("close failed");
		databaseFacade.MRV.setAlwaysThrowException("close", closeError);
		return closeError;
	}

	private void assertRecordLockException(Exception e, String message, Exception cause) {
		assertTrue(e instanceof RecordLockException);
		assertEquals(e.getMessage(), message);
		assertSame(e.getCause(), cause);
	}

	private void assertExecutedSqlWithTypeAndId(DatabaseFacadeSpy facade, int callNumber,
			String sql) {
		facade.MCR.assertParameter("executeSqlWithValues", callNumber, "sql", sql);
		facade.MCR.assertParameterAsEqual("executeSqlWithValues", callNumber, "values",
				List.of(SOME_TYPE, SOME_ID));
	}

	private void throwConflictForNumberOfInserts(int numberOfConflicts) {
		AtomicInteger calls = new AtomicInteger();
		databaseFacade.MRV.setDefaultReturnValuesSupplier("executeSqlWithValues", () -> {
			if (calls.getAndIncrement() < numberOfConflicts) {
				throw SqlConflictException.withMessage("duplicate key");
			}
			return 1;
		});
	}
}
