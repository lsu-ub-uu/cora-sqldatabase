/*
 * Copyright 2018, 2019 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.data;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.SqlConnectionProviderSpy;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.connection.ConnectionSpy;
import se.uu.ub.cora.sqldatabase.connection.PreparedStatementSpy;
import se.uu.ub.cora.sqldatabase.connection.ResultSetSpy;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;

public class DataReaderImpTest {
	private static final String SOME_SQL = "select x from y";
	private static final String ERROR_READING_DATA_USING_SQL = "Error reading data using sql: ";
	private DataReaderImp dataReader;
	private SqlConnectionProviderSpy sqlConnectionProviderSpy;
	private List<Object> values;
	private LoggerFactorySpy loggerFactorySpy;
	private String testedClassName = "DataReaderImp";

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);

		sqlConnectionProviderSpy = new SqlConnectionProviderSpy();
		dataReader = DataReaderImp.usingSqlConnectionProvider(sqlConnectionProviderSpy);
		values = new ArrayList<>();
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
	public void testGetSqlConnectionProvider() {
		assertEquals(dataReader.getSqlConnectionProvider(), sqlConnectionProviderSpy);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL + ": no row returned")
	public void testReadOneNoResultsThrowsException() throws Exception {
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadOneSqlErrorThrowsError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		try {
			dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testSqlPassedOnToConnectionForOneString() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		dataReader.readOneRowOrFailUsingSqlAndValues(
				"select * from someTableName where alpha2code = ?", values);
		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, "select * from someTableName where alpha2code = ?");
	}

	@Test
	public void testExecuteQueryForOneIsCalled() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValueFromConditions() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
	}

	@Test
	public void testExecuteQueryForOneIsCalledUsingValuesFromConditions() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		values.add("SWE");
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testCloseOfConnectionIsCalledAfterReadOne() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		ConnectionSpy connectionSpy = sqlConnectionProviderSpy.connection;
		assertTrue(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledAfterReadOne() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledAfterReadOne() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfResultSetContainsDataForOneGetResultSetMetadataIsCalled() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
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

	@Test
	public void testIfResultSetContainsDataForOneReturnedDataHasKeysFromResultSet()
			throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		setValuesInResultSetSpy(resultSetSpy);

		Row readRow = dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);

		assertEquals(readRow.columnSet().size(), 4);
		assertTrue(readRow.hasColumn("someColumnName"));
	}

	@Test
	public void testIfResultSetContainsDataForOneContainsExpectedKeys() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = createListOfRowValues(columnNames);
		resultSetSpy.rowValues = rowValues;

		Row readRow = dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
		assertEquals(readRow.columnSet().size(), 4);
		assertTrue(readRow.hasColumn(columnNames.get(0)));
		assertTrue(readRow.hasColumn(columnNames.get(1)));
		assertTrue(readRow.hasColumn(columnNames.get(2)));
		assertTrue(readRow.hasColumn(columnNames.get(3)));
	}

	@Test
	public void testIfResultSetContainsDataForOneContainsExptectedValues() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = createListOfRowValues(columnNames);
		resultSetSpy.rowValues = rowValues;

		Row readRow = dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
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
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();
		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);
		Map<String, Object> columnValues2 = createMapWithColumnNamesAndValues(columnNames, "2");
		rowValues.add(columnValues2);
		resultSetSpy.rowValues = rowValues;

		dataReader.readOneRowOrFailUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadFromTableUsingConditionNoResultsReturnsEmptyList() throws Exception {
		List<Row> results = dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL,
				values);
		assertEquals(results, Collections.emptyList());
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ ERROR_READING_DATA_USING_SQL + SOME_SQL)
	public void testReadFromTableUsingConditionSqlErrorThrowsError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorThrowsErrorAndSendsAlongOriginalError()
			throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		try {
			dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testReadFromTableUsingConditionSqlErrorLogs() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
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
			dataReader.executePreparedStatementQueryUsingSqlAndValues(sql, values);
		} catch (Exception e) {
			caughtException = e;
		}
		assertNotNull(caughtException);
	}

	@Test
	public void testSqlSetAsPreparedStatement() throws Exception {
		dataReader.executePreparedStatementQueryUsingSqlAndValues(
				"select * from someTableName where alpha2code = ?", values);
		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, "select * from someTableName where alpha2code = ?");
	}

	@Test
	public void testExecuteQueryIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.executeQueryWasCalled);
	}

	@Test
	public void testCloseOfConnectionIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		ConnectionSpy connectionSpy = sqlConnectionProviderSpy.connection;
		assertTrue(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfResultSetIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		assertTrue(resultSetSpy.closeWasCalled);
	}

	@Test
	public void testIfSetMetadataIsCalledForExecutePreparedStatement() throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);

		resultSetSpy.hasNext = true;
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		assertEquals(resultSetSpy.getMetadataWasCalled, true);
	}

	@Test
	public void testIfResultSetContainsDataReturnedDataHasKeysFromResultSetForExecutePreparedStatement()
			throws Exception {
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();
		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);
		resultSetSpy.rowValues = rowValues;

		List<Row> readAllFromTable = dataReader
				.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
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
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		resultSetSpy.hasNext = true;
		List<String> columnNames = createListOfColumnNames();
		resultSetSpy.columnNames = columnNames;

		List<Map<String, Object>> rowValues = new ArrayList<>();

		Map<String, Object> columnValues = createMapWithColumnNamesAndValues(columnNames, "");
		rowValues.add(columnValues);

		resultSetSpy.rowValues = rowValues;

		List<Row> readAllFromTable = dataReader
				.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
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
		ResultSetSpy resultSetSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy.resultSet;
		setValuesInResultSetSpy(resultSetSpy);
		values.add("SE");
		values.add("SWE");
		dataReader.executePreparedStatementQueryUsingSqlAndValues(SOME_SQL, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

}