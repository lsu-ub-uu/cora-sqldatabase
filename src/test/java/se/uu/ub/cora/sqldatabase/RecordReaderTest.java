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

package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RecordReaderTest {
	private RecordReaderImp recordReader;
	private SqlConnectionProviderSpy sqlConnectionProviderSpy;
	private Map<String, Object> conditions;
	private DataReaderSpy dataReader;

	@BeforeMethod
	public void beforeMethod() {
		conditions = new HashMap<>();
		conditions.put("alpha2code", "SE");
		dataReader = new DataReaderSpy();
		sqlConnectionProviderSpy = new SqlConnectionProviderSpy();
		recordReader = RecordReaderImp.usingDataReader(dataReader);
	}

	@Test
	public void testReadAllResultsReturnsResultFromDataReader() throws Exception {
		String tableName = "someTableName";
		List<Map<String, Object>> results = recordReader.readAllFromTable(tableName);
		assertTrue(dataReader.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(dataReader.sql, "select * from someTableName");
		assertTrue(dataReader.values.isEmpty());

		assertEquals(results, dataReader.result);
	}

	@Test(expectedExceptions = SqlStorageException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadAllFromTableSqlErrorThrowsError() throws Exception {
		dataReader.throwError = true;
		sqlConnectionProviderSpy.returnErrorConnection = true;
		recordReader = RecordReaderImp.usingDataReader(dataReader);
		recordReader.readAllFromTable("someTableName");
	}

	@Test
	public void testReadAllResultsReturnsResultFromDataReaderWithFilter() throws Exception {
		String tableName = "someTableName";
		ResultDelimiter resultDelimiter = new ResultDelimiter(100, 10);

		List<Map<String, Object>> results = recordReader.readAllFromTable(tableName,
				resultDelimiter);
		assertTrue(dataReader.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(dataReader.sql, "select * from someTableName limit 100 offset 10");
		assertTrue(dataReader.values.isEmpty());

		assertEquals(results, dataReader.result);
	}

	@Test
	public void testReadAllSqlWhenLimitIsNull() throws Exception {
		String tableName = "someTableName";
		ResultDelimiter resultDelimiter = new ResultDelimiter(null, 10);

		recordReader.readAllFromTable(tableName, resultDelimiter);
		assertEquals(dataReader.sql, "select * from someTableName offset 10");

	}

	@Test
	public void testReadAllSqlWhenOffsetIsNull() throws Exception {
		String tableName = "someTableName";
		ResultDelimiter resultDelimiter = new ResultDelimiter(100, null);

		recordReader.readAllFromTable(tableName, resultDelimiter);
		assertEquals(dataReader.sql, "select * from someTableName limit 100");

	}

	@Test
	public void testReadSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		recordReader = RecordReaderImp.usingDataReader(dataReader);
		try {
			recordReader.readAllFromTable("someTableName");
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testGeneratedSqlQueryString() throws Exception {
		recordReader.readAllFromTable("someTableName");
		assertEquals(dataReader.sql, "select * from someTableName");
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		recordReader = RecordReaderImp.usingDataReader(dataReader);
		try {
			recordReader.readOneRowFromDbUsingTableAndConditions("someTableName", conditions);
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testGeneratedSqlQueryForOneString() throws Exception {

		Map<String, Object> result = recordReader
				.readOneRowFromDbUsingTableAndConditions("someTableName", conditions);
		assertTrue(dataReader.readOneRowFromDbUsingTableAndConditionsWasCalled);

		assertEquals(dataReader.sql, "select * from someTableName where alpha2code = ?");
		List<Object> values = dataReader.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "SE");

		assertSame(result, dataReader.oneRowResult);

	}

	@Test
	public void testGeneratedSqlQueryForOneStringTwoConditions() throws Exception {
		conditions.put("alpha3code", "SWE");
		Map<String, Object> result = recordReader
				.readOneRowFromDbUsingTableAndConditions("someTableName", conditions);

		assertEquals(dataReader.sql,
				"select * from someTableName where alpha2code = ? and alpha3code = ?");
		List<Object> values = dataReader.values;
		assertEquals(values.size(), 2);
		assertEquals(values.get(0), "SE");
		assertEquals(values.get(1), "SWE");
		assertSame(result, dataReader.oneRowResult);
	}

	@Test(expectedExceptions = SqlStorageException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadOneRowFromDbUsingTableAndConditionsThrowError() throws Exception {

		dataReader.throwError = true;

		recordReader.readOneRowFromDbUsingTableAndConditions("someTableName", conditions);
	}

	@Test
	public void testReadFromTableUsingConditionReturnsResultFromDataReader() throws Exception {
		List<Map<String, Object>> result = recordReader
				.readFromTableUsingConditions("someTableName", conditions);

		assertTrue(dataReader.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(dataReader.sql, "select * from someTableName where alpha2code = ?");

		List<Object> values = dataReader.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "SE");

		assertSame(result, dataReader.result);
	}

	@Test
	public void testReadFromTableUsingTwoConditionReturnsResultFromDataReader() throws Exception {
		conditions.put("alpha3code", "SWE");
		List<Map<String, Object>> result = recordReader
				.readFromTableUsingConditions("someTableName", conditions);

		assertTrue(dataReader.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(dataReader.sql,
				"select * from someTableName where alpha2code = ? and alpha3code = ?");

		List<Object> values = dataReader.values;
		assertEquals(values.size(), 2);
		assertEquals(values.get(0), "SE");
		assertEquals(values.get(1), "SWE");

		assertSame(result, dataReader.result);
	}

	@Test(expectedExceptions = SqlStorageException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadFromTableUsingConditionSqlErrorThrowsError() throws Exception {
		dataReader.throwError = true;
		recordReader = RecordReaderImp.usingDataReader(dataReader);
		recordReader.readFromTableUsingConditions("someTableName", conditions);
	}

	@Test
	public void testReadNextFromSequence() {
		String sequenceName = "someSequence";
		Map<String, Object> result = recordReader.readNextValueFromSequence(sequenceName);
		assertTrue(dataReader.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertTrue(dataReader.values.isEmpty());
		assertEquals(dataReader.sql, "select nextval('someSequence')");

		assertSame(result, dataReader.oneRowResult);
	}

	@Test
	public void testReadNumberOfRows() {
		String type = "organisation";
		Map<String, Object> conditions = new HashMap<>();
		conditions.put("domain", "uu");

		long numberOfRows = recordReader.readNumberOfRows(type, conditions);

		assertTrue(dataReader.readOneRowFromDbUsingTableAndConditionsWasCalled);
		// assertTrue(dataReader.values.isEmpty());
		assertEquals(dataReader.sql, "select count(*) from organisation where domain = ?");
		List<Object> values = dataReader.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "uu");
		assertEquals(numberOfRows, dataReader.oneRowResult.get("count"));

	}

	@Test
	public void testReadNumberOfRowsNoConditions() {
		String type = "organisation";
		Map<String, Object> conditions = new HashMap<>();

		long numberOfRows = recordReader.readNumberOfRows(type, conditions);

		assertTrue(dataReader.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertEquals(dataReader.sql, "select count(*) from organisation");
		assertTrue(dataReader.values.isEmpty());
		assertEquals(numberOfRows, dataReader.oneRowResult.get("count"));

	}

}