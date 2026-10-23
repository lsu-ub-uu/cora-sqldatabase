/*
 * Copyright 2018, 2019, 2021, 2026 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.table.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.util.Collections;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.SqlTransactionException;
import se.uu.ub.cora.sqldatabase.sequence.DatabaseFacadeSpy;
import se.uu.ub.cora.sqldatabase.sequence.RowSpy;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public class TableFacadeTest {
	private TableFacade tableFacade;
	private TableQuerySpy tableQuerySpy;
	private DatabaseFacadeSpy databaseFacadeSpy;
	private RowSpy rowSpy;

	@BeforeMethod
	public void beforeMethod() {
		tableQuerySpy = new TableQuerySpy();
		rowSpy = new RowSpy();
		databaseFacadeSpy = new DatabaseFacadeSpy();
		databaseFacadeSpy.MRV.setDefaultReturnValuesSupplier("readOneRowOrFailUsingSqlAndValues",
				() -> rowSpy);
		tableFacade = TableFacadeImp.usingDatabaseFacade(databaseFacadeSpy);
	}

	@Test
	public void testTableFacadeUsesAutoclosable() {
		assertTrue(tableFacade instanceof AutoCloseable);
	}

	@Test
	public void testCloseCallsCloseInDbFacade() {
		tableFacade.close();
		databaseFacadeSpy.MCR.assertMethodWasCalled("close");
	}

	@Test
	public void testStartTransactionCallsDbFacade() {
		tableFacade.startTransaction();
		databaseFacadeSpy.MCR.assertMethodWasCalled("startTransaction");
	}

	@Test
	public void testEndTransactionCallsDbFacade() {
		tableFacade.endTransaction();
		databaseFacadeSpy.MCR.assertMethodWasCalled("endTransaction");
	}

	@Test
	public void testRollback() {
		tableFacade.rollback();
		databaseFacadeSpy.MCR.assertMethodWasCalled("rollback");
	}

	@Test
	public void testReadSqlErrorThrowsErrorAndSendsAlongOriginalError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("readUsingSqlAndValues");
		try {
			tableFacade.readRowsForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error reading data using sql: sql for read from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadSqlTransactionErrorThrowsErrorAndSendsAlongOriginalError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod("readUsingSqlAndValues");
		try {
			tableFacade.readRowsForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(), "Error reading data using sql: sql for read from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadRowsForQuery() {
		List<Row> results = tableFacade.readRowsForQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertParameters("readUsingSqlAndValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleReadSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));

		databaseFacadeSpy.MCR.assertReturn("readUsingSqlAndValues", 0, results);
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("readOneRowOrFailUsingSqlAndValues");

		try {
			tableFacade.readOneRowForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error reading one row using sql: sql for read from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadOneSqlTransactionErrorThrowsErrorAndSendsAlongOriginalError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod(
				"readOneRowOrFailUsingSqlAndValues");
		try {
			tableFacade.readOneRowForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(), "Error reading one row using sql: sql for read from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadOneRowForQuery() {
		Row results = tableFacade.readOneRowForQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleReadSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));

		databaseFacadeSpy.MCR.assertReturn("readOneRowOrFailUsingSqlAndValues", 0, results);
	}

	@Test
	public void testReadNumberOfRowsSqlErrorThrowsError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("readOneRowOrFailUsingSqlAndValues");

		try {
			tableFacade.readNumberOfRows(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(),
					"Error reading number of rows using sql: sql for count from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadNumberOfRowsSqlTransactionErrorThrowsError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod(
				"readOneRowOrFailUsingSqlAndValues");
		try {
			tableFacade.readNumberOfRows(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(),
					"Error reading number of rows using sql: sql for count from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testReadNumberOfRows() {
		long count = 423L;
		rowSpy.MRV.setSpecificReturnValuesSupplier("getValueByColumn", () -> count, "count");

		long numberOfRows = tableFacade.readNumberOfRows(tableQuerySpy);

		databaseFacadeSpy.MCR.assertCalledParametersReturn("readOneRowOrFailUsingSqlAndValues",
				tableQuerySpy.MCR.getReturnValue("assembleCountSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
		assertEquals(numberOfRows, count);
	}

	@Test
	public void testDeleteWithError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("executeSqlWithValues");

		try {
			tableFacade.deleteRowsForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error deleting rows using sql: sql for delete from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testDeleteWithTransactionError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod("executeSqlWithValues");
		try {
			tableFacade.deleteRowsForQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(), "Error deleting rows using sql: sql for delete from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testDeleteRowsForQuery() {
		int count = 423;
		databaseFacadeSpy.MRV.setDefaultReturnValuesSupplier("executeSqlWithValues", () -> count);

		int deletedRows = tableFacade.deleteRowsForQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertReturn("executeSqlWithValues", 0, deletedRows);
		databaseFacadeSpy.MCR.assertParameters("executeSqlWithValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleDeleteSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
	}

	@Test
	public void testInsertWithError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("executeSqlWithValues");

		try {
			tableFacade.insertRowUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error inserting row using sql: sql for create from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testInsertWithDuplicatedKeyError() {
		var error = setupDatabaseFasadeToThrowSqlConclictOnMethod("executeSqlWithValues");
		try {
			tableFacade.insertRowUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlConflictException);
			assertEquals(e.getMessage(),
					"Error inserting row, duplicated key, using sql: sql for create from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testInsertWithTransactionError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod("executeSqlWithValues");
		try {
			tableFacade.insertRowUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(), "Error inserting row using sql: sql for create from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testInsertRowsForQuery() {
		tableFacade.insertRowUsingQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertParameters("executeSqlWithValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleCreateSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
	}

	@Test
	public void testUpdateWithError() {
		var error = setupDatabaseFasadeToThrowSqlErrorOnMethod("executeSqlWithValues");

		try {
			tableFacade.updateRowsUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error updating rows using sql: sql for update from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testUpdateWithDuplicatedKeyError() {
		var error = setupDatabaseFasadeToThrowSqlConclictOnMethod("executeSqlWithValues");

		try {
			tableFacade.updateRowsUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlConflictException);
			assertEquals(e.getMessage(),
					"Error updating rows, duplicated key, using sql: sql for update from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testUpdateWithTransactionError() {
		var error = setupDatabaseFasadeToThrowSqlTransactionOnMethod("executeSqlWithValues");
		try {
			tableFacade.updateRowsUsingQuery(tableQuerySpy);
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlTransactionException);
			assertEquals(e.getMessage(), "Error updating rows using sql: sql for update from spy");
			assertEquals(e.getCause(), error);
		}
	}

	@Test
	public void testUpdateRowsForQuery() {
		int count = 423;
		databaseFacadeSpy.MRV.setDefaultReturnValuesSupplier("executeSqlWithValues", () -> count);

		int updateRows = tableFacade.updateRowsUsingQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertReturn("executeSqlWithValues", 0, updateRows);
		databaseFacadeSpy.MCR.assertParameters("executeSqlWithValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleUpdateSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
	}

	@Test(expectedExceptions = SqlDatabaseException.class)
	public void testReadNextFromSequenceError() {
		setupDatabaseFasadeToThrowSqlErrorOnMethod("readOneRowOrFailUsingSqlAndValues");

		tableFacade.nextValueFromSequence("someSequence");
	}

	@Test
	public void testReadNextFromSequence() {
		long count = 423L;
		rowSpy.MRV.setSpecificReturnValuesSupplier("getValueByColumn", () -> count, "nextval");

		long result = tableFacade.nextValueFromSequence("someSequence");

		databaseFacadeSpy.MCR.assertCalledParameters("readOneRowOrFailUsingSqlAndValues",
				"select nextval('someSequence') as nextval", Collections.emptyList());
		assertEquals(result, count);
	}

	private SqlDatabaseException setupDatabaseFasadeToThrowSqlErrorOnMethod(String method) {
		SqlDatabaseException error = SqlDatabaseException.withMessage("Some db error!");
		databaseFacadeSpy.MRV.setAlwaysThrowException(method, error);
		return error;
	}

	private SqlConflictException setupDatabaseFasadeToThrowSqlConclictOnMethod(String method) {
		SqlConflictException error = SqlConflictException.withMessage("Some conflict error!");
		databaseFacadeSpy.MRV.setAlwaysThrowException(method, error);
		return error;
	}

	private SqlTransactionException setupDatabaseFasadeToThrowSqlTransactionOnMethod(
			String method) {
		SqlTransactionException error = SqlTransactionException
				.withMessage("Some transaction error!");
		databaseFacadeSpy.MRV.setAlwaysThrowException(method, error);
		return error;
	}
}