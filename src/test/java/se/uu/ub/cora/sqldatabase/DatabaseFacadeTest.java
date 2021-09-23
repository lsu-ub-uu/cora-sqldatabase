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
import se.uu.ub.cora.sqldatabase.connection.PreparedStatementSpy;
import se.uu.ub.cora.sqldatabase.connection.ResultSetSpy;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;

public class DatabaseFacadeTest {
	private static final String ERROR_READING_DATA_USING_SQL = "Error reading data using sql: ";
	private DatabaseFacadeImp databaseFacade;
	private SqlConnectionProviderSpy sqlConnectionProviderSpy;
	private List<Object> values;
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "DatabaseFacadeImp";
	private static final String SOME_SQL = "select x from y";
	private static final String SELECT_SQL = "select * from someTableName where alpha2code = ?";;
	private static final String UPDATE_SQL = "update testTable set x=? where y = ?";
	private PreparedStatementSpy preparedStatementSpy;
	private ResultSetSpy resultSetSpy;
	private ConnectionSpy connectionSpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		values = new ArrayList<>();
		setupConnectionSpies();
		databaseFacade = DatabaseFacadeImp.usingSqlConnectionProvider(sqlConnectionProviderSpy);
	}

	private void setupConnectionSpies() {
		sqlConnectionProviderSpy = new SqlConnectionProviderSpy();
		preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		resultSetSpy = preparedStatementSpy.resultSet;
		connectionSpy = sqlConnectionProviderSpy.connection;
	}

	@Test
	public void testDatabaseFacadeExtendsAutoclosable() throws Exception {
		assertTrue(databaseFacade instanceof AutoCloseable);
	}

	@Test
	public void testCloseThrowsExceptionIfCloseFails() throws Exception {
		databaseFacade.startTransaction();
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.close();
			makeSureErrorIsThrownFromAboveStatements();
		} catch (Exception e) {
			assertTrue(e instanceof SqlDatabaseException);
			assertEquals(e.getMessage(), "Error closing connection.");
			assertEquals(e.getCause().getMessage(), "error thrown from close in ConnectionSpy");
		}
	}

	@Test
	public void testCloseClosesConnection() throws Exception {
		databaseFacade.startTransaction();
		databaseFacade.close();
		assertTrue(connectionSpy.closeWasCalled);
	}

	@Test
	public void testGetSqlConnectionProvider() {
		assertEquals(databaseFacade.getSqlConnectionProvider(), sqlConnectionProviderSpy);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL + ": no row returned")
	public void testReadOneNoResultsThrowsException() throws Exception {
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadOneSqlErrorThrowsError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
			makeSureErrorIsThrownFromAboveStatements();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testSqlPassedOnToConnectionForOneString() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SELECT_SQL, values);

		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, SELECT_SQL);
	}

	@Test
	public void testOnlyOneConnectionIsUsed() throws Exception {
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
	public void testExecuteQueryForOneIsCalled() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValueFromConditions() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValuesFromConditions() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		values.add("SWE");
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testCloseOfConnectionIsNotCalledAfterReadOne() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);

		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledAfterReadOne() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledAfterReadOne() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfResultSetContainsDataForOneGetResultSetMetadataIsCalled() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(resultSetSpy.getMetadataWasCalled, true);
	}

	private void setValuesInResultSetSpy(ResultSetSpy resultSetSpy) {
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
	public void testIfResultSetContainsDataForOneReturnedDataHasKeysFromResultSet()
			throws Exception {
		resultSetSpy.hasNext = true;
		setValuesInResultSetSpy(resultSetSpy);

		Row readRow = databaseFacade.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);

		assertEquals(readRow.columnSet().size(), 4);
		assertTrue(readRow.hasColumn("someColumnName"));
	}

	@Test
	public void testIfResultSetContainsDataForOneContainsExpectedKeys() throws Exception {
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
	public void testIfResultSetContainsDataForOneContainsExptectedValues() throws Exception {
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

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL + ": more than one row returned")
	public void testIfResultSetContainsDataForOneMoreRowsDataReturnedDataContainsValuesFromResultSet()
			throws Exception {
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
	public void testReadFromTableUsingConditionNoResultsReturnsEmptyList() throws Exception {
		List<Row> results = databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertEquals(results, Collections.emptyList());
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadFromTableUsingConditionSqlErrorThrowsError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorThrowsErrorAndSendsAlongOriginalError()
			throws Exception {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
			makeSureErrorIsThrownFromAboveStatements();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorLogs() throws Exception {
		connectionSpy.throwErrorConnection = true;
		executePreparedStatementUsingSqlAndValuesMakeSureErrorIsThrown(SOME_SQL, null);
		assertEquals(loggerFactorySpy.getErrorLogMessageUsingClassNameAndNo(testedClassName, 0),
				ERROR_READING_DATA_USING_SQL + SOME_SQL);
		Exception exceptionInErrorLog = loggerFactorySpy
				.getErrorLogExceptionsUsingClassNameAndNo(testedClassName, 0);
		assertNotNull(exceptionInErrorLog);
		assertEquals(exceptionInErrorLog.getMessage(), "error thrown from prepareStatement in spy");
	}

	private void executePreparedStatementUsingSqlAndValuesMakeSureErrorIsThrown(String sql,
			List<Object> values) {
		Exception caughtException = null;
		try {
			databaseFacade.readUsingSqlAndValues(sql, values);
			makeSureErrorIsThrownFromAboveStatements();
		} catch (Exception e) {
			caughtException = e;
		}
		assertNotNull(caughtException);
	}

	@Test
	public void testSqlSetAsPreparedStatement() throws Exception {
		databaseFacade.readUsingSqlAndValues(SELECT_SQL, values);
		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, SELECT_SQL);
	}

	@Test
	public void testExecuteQueryIsCalledForExecutePreparedStatement() throws Exception {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testCloseOfConnectionIsNotCalledForExecutePreparedStatement() throws Exception {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);

		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledForExecutePreparedStatement() throws Exception {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledForExecutePreparedStatement() throws Exception {
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfSetMetadataIsCalledForExecutePreparedStatement() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		databaseFacade.readUsingSqlAndValues(SOME_SQL, values);
		assertEquals(resultSetSpy.getMetadataWasCalled, true);
	}

	@Test
	public void testIfResultSetContainsDataReturnedDataHasKeysFromResultSetForExecutePreparedStatement()
			throws Exception {

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
	public void testIfResultSetContainsDataReturnedDataContainsValuesFromResultSetForExecutePreparedStatement()
			throws Exception {
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
	public void testUsingValuesFromConditionsForExecutePreparedStatement() throws Exception {
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
	public void testExecuteSqlThrowsError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
	}

	@Test
	public void testExecuteSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.executeSqlWithValues(UPDATE_SQL, values);
			makeSureErrorIsThrownFromAboveStatements();
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	private void makeSureErrorIsThrownFromAboveStatements() {
		assertTrue(false);
	}

	@Test
	public void testExecuteUsingSqlAndValuesClosesPrepareStatmentsAndConnection() throws Exception {
		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		ConnectionSpy connectionSpy = sqlConnectionProviderSpy.connection;

		assertEquals(generatedSql, UPDATE_SQL);
		assertTrue(preparedStatementSpy.executeUpdateWasCalled);
		assertTrue(preparedStatementSpy.closeWasCalled);
		// assertTrue(connectionSpy.closeWasCalled);
		sqlConnectionProviderSpy.MCR.assertNumberOfCallsToMethod("getConnection", 1);
		assertFalse(connectionSpy.closeWasCalled);
	}

	@Test
	public void testExecuteSqlWithValuesAreSetInSql() throws Exception {
		values.add("SE");
		values.add("SWE");

		databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testExecuteSqlWithValuesNoOfAffectedRowsAreReturned() throws Exception {
		preparedStatementSpy.noOfAffectedRows = 5;
		values.add("SE");
		values.add("SWE");

		int updatedRows = databaseFacade.executeSqlWithValues(UPDATE_SQL, values);

		assertEquals(updatedRows, 5);
	}

	@Test
	public void testSetTimestampPreparedStatement() throws Exception {
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
	public void testReadUsingSqlAndValuesWithDatabaseNull() throws Exception {
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.readUsingSqlAndValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	private List<Object> prepareValuesWithDatabaseNullValue() {
		List<Object> valuesWithNull = new ArrayList<>();
		valuesWithNull.add(new DatabaseNull());
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
	public void testReadOneRowOrFailUsingSqlAndValuesWithDatabaseNull() throws Exception {
		setValuesInResultSetSpy(resultSetSpy);
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.readOneRowOrFailUsingSqlAndValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	@Test
	public void testExecuteSqlWithValuesWithDatabaseNull() throws Exception {
		List<Object> valuesWithNull = prepareValuesWithDatabaseNullValue();

		databaseFacade.executeSqlWithValues(SELECT_SQL, valuesWithNull);

		assertDatabaseNullValue();
	}

	@Test
	public void testStartTransactionThrowsError() throws Exception {
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.startTransaction();
			makeSureErrorIsThrownFromAboveStatements();
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
	public void testEndTransactionThrowsError() throws Exception {
		databaseFacade.startTransaction();
		connectionSpy.throwErrorConnection = true;
		try {
			databaseFacade.endTransaction();
			makeSureErrorIsThrownFromAboveStatements();
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