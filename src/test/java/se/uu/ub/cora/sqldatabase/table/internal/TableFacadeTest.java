/*
 * Copyright 2018, 2019, 2021 Uppsala University Library
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

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.OldDatabaseFacadeSpy;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public class TableFacadeTest {
	private TableFacade tableFacade;
	private OldDatabaseFacadeSpy databaseFacadeSpy;
	private TableQuerySpy tableQuerySpy;

	@BeforeMethod
	public void beforeMethod() {
		tableQuerySpy = new TableQuerySpy();
		databaseFacadeSpy = new OldDatabaseFacadeSpy();
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
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.readRowsForQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error reading data using sql: sql for read from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executePreparedStatementQueryUsingSqlAndValues in DatabaseFacadeSpy");
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
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.readOneRowForQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error reading one row using sql: sql for read from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from readOneRowOrFailUsingSqlAndValues in DatabaseFacadeSpy");
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
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.readNumberOfRows(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(),
					"Error reading number of rows using sql: sql for count from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from readOneRowOrFailUsingSqlAndValues in DatabaseFacadeSpy");
		}
	}

	@Test
	public void testReadNumberOfRows() {
		long numberOfRows = tableFacade.readNumberOfRows(tableQuerySpy);

		databaseFacadeSpy.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleCountSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));

		Row row = (Row) databaseFacadeSpy.MCR.getReturnValue("readOneRowOrFailUsingSqlAndValues",
				0);
		assertEquals(numberOfRows, (long) row.getValueByColumn("count"));
	}

	@Test
	public void testDeleteWithError() {
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.deleteRowsForQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error deleting rows using sql: sql for delete from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executeSqlWithValues in DatabaseFacadeSpy");
		}
	}

	@Test
	public void testDeleteRowsForQuery() {
		databaseFacadeSpy.numberOfAffectedRows = 23;

		int deletedRows = tableFacade.deleteRowsForQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertReturn("executeSqlWithValues", 0, deletedRows);
		databaseFacadeSpy.MCR.assertParameters("executeSqlWithValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleDeleteSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
	}

	@Test
	public void testInsertWithError() {
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.insertRowUsingQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error inserting row using sql: sql for create from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executeSqlWithValues in DatabaseFacadeSpy");
		}
	}

	@Test
	public void testInsertWithDuplicatedKeyError() {
		databaseFacadeSpy.throwDuplicatedKeyError = true;
		try {
			tableFacade.insertRowUsingQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof SqlConflictException);
			assertEquals(e.getMessage(),
					"Error inserting row, duplicated key, using sql: sql for create from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executeSqlWithValues in DatabaseFacadeSpy");
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
		databaseFacadeSpy.throwError = true;
		try {
			tableFacade.updateRowsUsingQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error updating rows using sql: sql for update from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executeSqlWithValues in DatabaseFacadeSpy");
		}
	}

	@Test
	public void testUpdateWithDuplicatedKeyError() {
		databaseFacadeSpy.throwDuplicatedKeyError = true;
		try {
			tableFacade.updateRowsUsingQuery(tableQuerySpy);
			assertTrue(false);
		} catch (Exception e) {
			assertTrue(e instanceof SqlConflictException);
			assertEquals(e.getMessage(),
					"Error updating rows, duplicated key, using sql: sql for update from spy");
			assertEquals(e.getCause().getMessage(),
					"Error from executeSqlWithValues in DatabaseFacadeSpy");
		}
	}

	@Test
	public void testUpdateRowsForQuery() {
		databaseFacadeSpy.numberOfAffectedRows = 19;

		int updateRows = tableFacade.updateRowsUsingQuery(tableQuerySpy);

		databaseFacadeSpy.MCR.assertReturn("executeSqlWithValues", 0, updateRows);
		databaseFacadeSpy.MCR.assertParameters("executeSqlWithValues", 0,
				tableQuerySpy.MCR.getReturnValue("assembleUpdateSql", 0),
				tableQuerySpy.MCR.getReturnValue("getQueryValues", 0));
	}

	@Test(expectedExceptions = SqlDatabaseException.class)
	public void testReadNextFromSequenceError() {
		databaseFacadeSpy.throwError = true;

		tableFacade.nextValueFromSequence("someSequence");
	}

	@Test
	public void testReadNextFromSequence() {
		long result = tableFacade.nextValueFromSequence("someSequence");
		assertTrue(databaseFacadeSpy.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertTrue(databaseFacadeSpy.values.isEmpty());
		assertEquals(databaseFacadeSpy.sql, "select nextval('someSequence') as nextval");
		long resultInSpy = 438234090L;
		assertEquals(result, resultInSpy);
	}
}