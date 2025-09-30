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

package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.connection.ConnectionSpy;
import se.uu.ub.cora.sqldatabase.connection.OldConnectionSpy;
import se.uu.ub.cora.sqldatabase.connection.OldPreparedStatementSpy;
import se.uu.ub.cora.sqldatabase.connection.OldResultSetSpy;
import se.uu.ub.cora.sqldatabase.connection.PreparedStatementSpy;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.testspies.logger.LoggerFactorySpy;
import se.uu.ub.cora.testspies.logger.LoggerSpy;

public class DatabaseFacadeTest {
	private static final String ERROR_READING_DATA_USING_SQL = "Error reading data using sql: ";
	private DatabaseFacade databaseFacade;
	private OldSqlConnectionProviderSpy sqlConnectionProviderSpy;
	private List<Object> values;
	private LoggerFactorySpy loggerFactorySpy;
	private static final String SOME_SQL = "select x from y";
	private static final String SELECT_SQL = "select * from someTableName where alpha2code = ?";;
	private static final String UPDATE_SQL = "update testTable set x=? where y = ?";
	private OldPreparedStatementSpy preparedStatementSpy;
	private OldResultSetSpy resultSetSpy;
	private OldConnectionSpy connectionSpy;
	private ConnectionSpy connection;
	private SqlConnectionProviderSpy sqlConnectionProvider;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		values = new ArrayList<>();
		setupConnectionSpies();
		databaseFacade = DatabaseFacadeImp.usingSqlConnectionProvider(sqlConnectionProviderSpy);
	}

	private void setupConnectionSpies() {
		sqlConnectionProviderSpy = new OldSqlConnectionProviderSpy();
		connectionSpy = sqlConnectionProviderSpy.connection;
		preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		resultSetSpy = preparedStatementSpy.resultSet;
	}

	@Test
	public void testDatabaseFacadeExtendsAutoclosable() {
		assertTrue(databaseFacade instanceof AutoCloseable);
	}

	@Test
	public void testCloseThrowsExceptionIfCloseFails() {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.close();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlDatabaseException);
			assertEquals(e.getMessage(), "Error closing connection.");
			assertEquals(e.getCause().getMessage(), "error thrown from close in ConnectionSpy");
		}
	}

	@Test
	public void testCloseClosesConnection() {
		databaseFacade.startTransaction();
		databaseFacade.endTransaction();
		databaseFacade.close();
		assertTrue(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOnRunningTransactionShouldRollbackCloseAndThrowError() {
		try {
			databaseFacade.startTransaction();
			databaseFacade.close();
			fail();
		} catch (Exception e) {
			assertTrue(e instanceof SqlDatabaseException);
			assertEquals(e.getMessage(),
					"Close called on running transaction, rollback perfromed.");
			connectionSpy.MCR.assertMethodWasCalled("rollback");
			assertTrue(connectionSpy.closeWasCalled);
		}
	}

	@Test
	public void testCloseOnRunningTransactionShouldRollbackIfFailsCloseShouldBeCalled() {
		try {
			connectionSpy.throwErrorRollback = true;
			databaseFacade.startTransaction();
			databaseFacade.close();
			fail();
		} catch (Exception e) {
			connectionSpy.MCR.assertMethodWasCalled("rollback");
			assertTrue(connectionSpy.closeWasCalled);
			assertTrue(e instanceof SqlDatabaseException);
			assertEquals(e.getMessage(), "Error doing rollBack on connection.");
		}
	}

	@Test
	public void testCloseShouldCompleteWithoutConnection() {
		databaseFacade.close();
		assertTrue(true);
	}

	@Test
	public void testGetSqlConnectionProvider() {
		DatabaseFacadeImp databaseFacadeImp = (DatabaseFacadeImp) databaseFacade;
		assertEquals(databaseFacadeImp.getSqlConnectionProvider(), sqlConnectionProviderSpy);
	}

	@Test(expectedExceptions = SqlNotFoundException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL + ": no row returned")
	public void testReadOneNoResultsThrowsException() {
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadOneSqlErrorThrowsError() {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
			fail();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testSqlPassedOnToConnectionForOneString() {
		setValuesInResultSetSpy(resultSetSpy);

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SELECT_SQL, values);

		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, SELECT_SQL);
	}

	@Test
	public void testOnlyOneConnectionIsUsed() {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SELECT_SQL, values);
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		databaseFacade.startTransaction();
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
		databaseFacade.endTransaction();

		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
	}

	@Test
	public void testRollbackError() {
		connectionSpy.throwErrorRollback = true;
		try {
			databaseFacade.startTransaction();
			databaseFacade.rollback();
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error doing rollBack on connection.");
			assertEquals(e.getCause().getMessage(), "error thrown from rollback in ConnectionSpy");
		}
	}

	@Test
	public void testRollback() {
		databaseFacade.startTransaction();
		databaseFacade.rollback();
		connectionSpy.MCR.assertMethodWasCalled("rollback");
	}

	@Test
	public void testExecuteQueryThrowsException() {
		setUpSqlConnectionProvider();
		connection.MRV.setAlwaysThrowException("prepareStatement",
				new RuntimeException("someException"));
		DatabaseFacadeImp customDatabaseFacade = DatabaseFacadeImp
				.usingSqlConnectionProvider(sqlConnectionProvider);

		try {
			customDatabaseFacade.executeSql(SOME_SQL);
			fail();
		} catch (Exception e) {
			System.err.println(e);
			assertTrue(e instanceof SqlDatabaseException);
			assertEquals(e.getMessage(), "Error executing statement: " + SOME_SQL);
			assertEquals(e.getCause().getMessage(), "someException");
		}
	}

	@Test
	public void testExecuteQuery() {
		setUpSqlConnectionProvider();
		DatabaseFacadeImp customDatabaseFacade = DatabaseFacadeImp
				.usingSqlConnectionProvider(sqlConnectionProvider);

		customDatabaseFacade.executeSql(SOME_SQL);

		PreparedStatementSpy preparedStatement = assertConnectionCreatesPreparedStatement(
				sqlConnectionProvider, connection);
		preparedStatement.MCR.assertMethodWasCalled("execute");
	}

	private void setUpSqlConnectionProvider() {
		sqlConnectionProvider = new SqlConnectionProviderSpy();

		connection = new ConnectionSpy();
		sqlConnectionProvider.MRV.setDefaultReturnValuesSupplier("getConnection", () -> connection);
	}

	private PreparedStatementSpy assertConnectionCreatesPreparedStatement(
			SqlConnectionProviderSpy sqlConnectionProvider, ConnectionSpy connection) {
		sqlConnectionProvider.MCR.assertMethodWasCalled("getConnection");
		return (PreparedStatementSpy) connection.MCR
				.assertCalledParametersReturn("prepareStatement", SOME_SQL);
	}

	@Test
	public void testExecuteQueryForOneIsCalled() {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValueFromConditions() {
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValuesFromConditions() {
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		values.add("SWE");
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testCloseOfConnectionIsNotCalledAfterReadOne() {
		setValuesInResultSetSpy(resultSetSpy);

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);

		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledAfterReadOne() {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledAfterReadOne() {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfResultSetContainsDataForOneGetResultSetMetadataIsCalled() {
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(resultSetSpy.getMetadataWasCalled, true);
	}

	private void setValuesInResultSetSpy(OldResultSetSpy resultSetSpy) {
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = createListOfRowValues(columnNames);
		resultSetSpy.rowValues = rowValues;
	}

	private List<String> createListOfColumnNames() {
		List<String> columnNames = new ArrayList<>();
		columnNames.add("someColumnName");
		columnNames.add("someOtherColumnName");
		columnNames.add("twoColumnName");
		columnNames.add("someColumnNameThree");
		return columnNames;
	}

	private List<Map<String, Object>> createListOfRowValues(List<String> columnNames) {
		List<Map<String, Object>> rowValues = new ArrayList<>();
		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);
		return rowValues;
	}

	private Map<String, Object> createMapWithColumnNamesAndValues(List<String> columnNames,
			String extraValue) {
		Map<String, Object> columnValues = new HashMap<>();
		columnValues.put(columnNames.get(0), "value1" + extraValue);
		columnValues.put(columnNames.get(1), "secondValue" + extraValue);
		columnValues.put(columnNames.get(2), 3);
		columnValues.put(columnNames.get(3), "someOther value four" + extraValue);
		return columnValues;
	}

	@Test
	public void testIfResultSetContainsDataForOneReturnedDataHasKeysFromResultSet() {
		resultSetSpy.hasNext = true;
		setValuesInResultSetSpy(resultSetSpy);

		Row readRow = databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);

		assertEquals(readRow.columnSet().size(), 4);
		assertTrue(readRow.hasColumn("someColumnName"));
	}

	@Test
	public void testIfResultSetContainsDataForOneContainsExpectedKeys() {
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = createListOfRowValues(columnNames);
		resultSetSpy.rowValues = rowValues;

		Row readRow = databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(readRow.columnSet().size(), 4);
		assertTrue(readRow.hasColumn(columnNames.get(0)));
		assertTrue(readRow.hasColumn(columnNames.get(1)));
		assertTrue(readRow.hasColumn(columnNames.get(2)));
		assertTrue(readRow.hasColumn(columnNames.get(3)));
	}

	@Test
	public void testIfResultSetContainsDataForOneContainsExptectedValues() {
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = createListOfRowValues(columnNames);
		resultSetSpy.rowValues = rowValues;

		Row readRow = databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(readRow.columnSet().size(), 4);
		assertEquals(readRow.getValueByColumn(columnNames.get(0)), "value1");
		assertEquals(readRow.getValueByColumn(columnNames.get(1)), "secondValue");
		assertEquals(readRow.getValueByColumn(columnNames.get(2)), 3);
		assertEquals(readRow.getValueByColumn(columnNames.get(3)), "someOther value four");
	}

	@Test(expectedExceptions = SqlDataException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL + ": more than one row returned")
	public void testIfResultSetContainsDataForOneMoreRowsDataReturnedDataContainsValuesFromResultSet() {
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();
		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);
		Map<String, Object> columnValues2 = createMapWithColumnNamesAndValues(columnNames, "2");
		rowValues.add(columnValues2);
		resultSetSpy.rowValues = rowValues;

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadFromTableUsingConditionNoResultsReturnsEmptyList() {
		List<Row> results = databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertEquals(results, Collections.emptyList());
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadFromTableUsingConditionSqlErrorThrowsError() {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorThrowsErrorAndSendsAlongOriginalError() {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
			fail();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorLogs() {
		connectionSpy.throwErrorConnection = true;

		executePreparedStatementUsingSqlAndValuesMakeSureErrorIsThrown(SOME_SQL, null);

		LoggerSpy errorLogger = (LoggerSpy) loggerFactorySpy.MCR.getReturnValue("factorForClass",
				0);
		Object errorThrownFromConnectionSpy = connectionSpy.MCR.getReturnValue("prepareStatement",
				0);
		errorLogger.MCR.assertParameters("logErrorUsingMessageAndException", 0,
				ERROR_READING_DATA_USING_SQL + SOME_SQL, errorThrownFromConnectionSpy);
	}

	private void executePreparedStatementUsingSqlAndValuesMakeSureErrorIsThrown(String sql,
			List<Object> values) {
		Exception caughtException = null;
		try {
			databaseFacade.readUsingSqlAndValues(sql, values);
			fail();
		} catch (Exception e) {
			caughtException = e;
		}
		assertNotNull(caughtException);
	}

	@Test
	public void testSqlSetAsPreparedStatement() {
		databaseFacade.readUsingSqlAndValues(SELECT_SQL, values);
		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, SELECT_SQL);
	}

	@Test
	public void testExecuteQueryIsCalledForExecutePreparedStatement() {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testCloseOfConnectionIsNotCalledForExecutePreparedStatement() {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);

		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledForExecutePreparedStatement() {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledForExecutePreparedStatement() {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfSetMetadataIsCalledForExecutePreparedStatement() {
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertEquals(resultSetSpy.getMetadataWasCalled, true);
	}

	@Test
	public void testIfResultSetContainsDataReturnedDataHasKeysFromResultSetForExecutePreparedStatement() {

		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();
		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);
		resultSetSpy.rowValues = rowValues;

		List<Row> readAllFromTable = databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		Row row0 = readAllFromTable.get(0);

		Set<String> columnSet = row0.columnSet();
		assertEquals(columnSet.size(), 4);
		assertTrue(columnSet.contains(columnNames.get(0)));
		assertTrue(columnSet.contains(columnNames.get(1)));
		assertTrue(columnSet.contains(columnNames.get(2)));
		assertTrue(columnSet.contains(columnNames.get(3)));
	}

	@Test
	public void testIfResultSetContainsDataReturnedDataContainsValuesFromResultSetForExecutePreparedStatement() {
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();

		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);

		resultSetSpy.rowValues = rowValues;

		List<Row> readAllFromTable = databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		Row row0 = readAllFromTable.get(0);

		Set<String> columnSet = row0.columnSet();
		assertEquals(readAllFromTable.size(), 1);
		assertEquals(columnSet.size(), 4);
		assertEquals(row0.getValueByColumn(columnNames.get(0)), "value1");
		assertEquals(row0.getValueByColumn(columnNames.get(1)), "secondValue");
		assertEquals(row0.getValueByColumn(columnNames.get(2)), 3);
		assertEquals(row0.getValueByColumn(columnNames.get(3)), "someOther value four");
	}

	@Test
	public void testUsingValuesFromConditionsForExecutePreparedStatement() {
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		values.add("SWE");
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testNoAffectedRows() {
		int updatedRows = databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(updatedRows, 0);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Error executing statement: update testTable set x=\\? where y = \\?")
	public void testExecuteSqlThrowsError() {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
	}

	@Test
	public void testExecuteSqlThrowsSqlConflictException() {
		preparedStatementSpy.throwDuplicateKeyException = true;
		try {
			databaseFacade.executeSqlWithValues("someSQL", values);
			fail();

		} catch (Exception e) {
			assertTrue(e instanceof SqlConflictException);
			assertEquals(e.getMessage(), "Error executing statement, duplicated key: someSQL");
			assertEquals(e.getCause().getMessage(),
					"duplicate key value violates unique constraint \"organisation_pkey\"");
		}
	}

	@Test
	public void testExecuteSqlErrorThrowsErrorAndSendsAlongOriginalError() {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
			fail();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testExecuteUsingSqlAndValuesClosesPrepareStatmentsAndConnection() {
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		OldConnectionSpy connectionSpy = sqlConnectionProviderSpy.connection;

		assertEquals(generatedSql, UPDATE_SQL);
		assertTrue(preparedStatementSpy.executeUpdateWasCalled);
		assertTrue(preparedStatementSpy.closeWasCalled);
		// assertTrue(connectionSpy.closeWasCalled);
		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testExecuteSqlWithValuesAreSetInSql() {
		values.add("SE");
		values.add("SWE");

		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testExecuteSqlWithValuesNoOfAffectedRowsAreReturned() {
		preparedStatementSpy.noOfAffectedRows = 5;
		values.add("SE");
		values.add("SWE");

		int updatedRows = databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(updatedRows, 5);
	}

	@Test
	public void testSetTimestampPreparedStatement() {
		values.add("SE");
		values.add(createTimestamp());

		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertTrue(preparedStatementSpy.usedSetTimestamps.get("2") instanceof Timestamp);
	}

	private Timestamp createTimestamp() {
		Date today = new Date();
		long time = today.getTime();
		Timestamp timestamp = new Timestamp(time);
		return timestamp;
	}

	@Test
	public void testReadUsingSqlAndValuesWithDatabaseNull() {
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.readUsingSqlAndValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	private List<Object> prepareValuesWithDatabaseNullValue() {
		List<Object> valuesWithNull = new ArrayList<>();
		valuesWithNull.add(DatabaseValues.NULL);
		valuesWithNull.add("someValue");
		return valuesWithNull;
	}

	private void assertDatabaseNullValue() {
		int sqlNull = java.sql.Types.NULL;
		preparedStatementSpy.MCR.assertNumberOfCallsToMethod("setNull", 1);
		preparedStatementSpy.MCR.assertNumberOfCallsToMethod("setObject", 1);
		preparedStatementSpy.MCR.assertParameters("setNull", 0, 1, sqlNull);
		preparedStatementSpy.MCR.assertParameters("setObject", 0, 2, "someValue");
	}

	@Test
	public void testReadOneRowOrFailUsingSqlAndValuesWithDatabaseNull() {
		setValuesInResultSetSpy(resultSetSpy);
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	@Test
	public void testExecuteSqlWithValuesWithDatabaseNull() {
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.executeSqlWithValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	@Test
	public void testStartTransactionThrowsError() {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.startTransaction();
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error starting transaction.");
			assertEquals(e.getCause().getMessage(), "error thrown from setAutoCommit in spy");
		}
	}

	@Test
	public void testStartTransaction() throws Exception {
		databaseFacade.startTransaction();
		// assertTrue(connectionSpy.getAutoCommit());
		assertTrue(sqlConnectionProviderSpy.getConnectionHasBeenCalled);
		// assertFalse(connectionSpy.closeWasCalled);
		assertFalse(connectionSpy.getAutoCommit());
	}

	@Test
	public void testEndTransactionThrowsError() {
		databaseFacade.startTransaction();
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.endTransaction();
			fail();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error ending transaction.");
			assertEquals(e.getCause().getMessage(), "error thrown from setAutoCommit in spy");
		}
	}

	@Test
	public void testEndTransaction() throws Exception {
		databaseFacade.startTransaction();
		assertFalse(connectionSpy.getAutoCommit());
		databaseFacade.endTransaction();
		assertTrue(connectionSpy.getAutoCommit());
	}

}