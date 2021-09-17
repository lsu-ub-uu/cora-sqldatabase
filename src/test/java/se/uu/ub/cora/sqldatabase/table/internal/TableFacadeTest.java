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
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.Conditions;
import se.uu.ub.cora.sqldatabase.DbQueryInfoImp;
import se.uu.ub.cora.sqldatabase.DbQueryInfoSpy;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SortOrder;
import se.uu.ub.cora.sqldatabase.SqlConnectionProviderSpy;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.data.DatabaseFacadeSpy;
import se.uu.ub.cora.sqldatabase.internal.ConditionsImp;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public class TableFacadeTest {
	private TableFacade tableFacade;
	private SqlConnectionProviderSpy sqlConnectionProviderSpy;
	private Map<String, Object> values;
	private Conditions conditions;
	private DatabaseFacadeSpy databaseFacadeSpy;

	@BeforeMethod
	public void beforeMethod() {
		values = new HashMap<>();
		conditions = new ConditionsImp();
		databaseFacadeSpy = new DatabaseFacadeSpy();
		sqlConnectionProviderSpy = new SqlConnectionProviderSpy();
		tableFacade = TableFacadeImp.usingDataReader(databaseFacadeSpy);
	}

	@Test
	public void testReadAllResultsReturnsResultFromDataReader() throws Exception {
		String tableName = "someTableName";
		List<Row> results = tableFacade.readRowsFromTable(tableName);
		assertTrue(databaseFacadeSpy.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName");
		assertTrue(databaseFacadeSpy.values.isEmpty());

		assertEquals(results, databaseFacadeSpy.result);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadAllFromTableSqlErrorThrowsError() throws Exception {
		databaseFacadeSpy.throwError = true;
		sqlConnectionProviderSpy.returnErrorConnection = true;
		tableFacade = TableFacadeImp.usingDataReader(databaseFacadeSpy);
		tableFacade.readRowsFromTable("someTableName");
	}

	@Test
	public void testReadAllResultsReturnsResultFromDataReaderWithFilter() throws Exception {
		String tableName = "someTableName";
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(10, 109);

		List<Row> results = tableFacade.readRowsFromTable(tableName, queryInfo);
		assertTrue(databaseFacadeSpy.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName limit 100 offset 9");
		assertTrue(databaseFacadeSpy.values.isEmpty());

		assertEquals(results, databaseFacadeSpy.result);
	}

	@Test
	public void testReadAllSqlWhenLimitIsNull() throws Exception {
		String tableName = "someTableName";
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(10, null);

		tableFacade.readRowsFromTable(tableName, queryInfo);
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName offset 9");

	}

	@Test
	public void testReadAllSqlWhenOffsetIsNull() throws Exception {
		String tableName = "someTableName";
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(null, 100);

		tableFacade.readRowsFromTable(tableName, queryInfo);
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName limit 100");
	}

	@Test
	public void testReadAllSqlWhenOrderByAndSortOrderIsPresent() throws Exception {
		String tableName = "someTableName";
		DbQueryInfoSpy queryInfo = new DbQueryInfoSpy(10, 109);
		queryInfo.setOrderBy("organistion_id");
		queryInfo.setSortOrder(SortOrder.ASC);

		tableFacade.readRowsFromTable(tableName, queryInfo);
		assertEquals(databaseFacadeSpy.sql,
				"select * from someTableName order by from spy delimiter from spy");

	}

	@Test
	public void testReadSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		tableFacade = TableFacadeImp.usingDataReader(databaseFacadeSpy);
		try {
			tableFacade.readRowsFromTable("someTableName");
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testGeneratedSqlQueryString() throws Exception {
		tableFacade.readRowsFromTable("someTableName");
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName");
	}

	@Test
	public void testReadOneSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		tableFacade = TableFacadeImp.usingDataReader(databaseFacadeSpy);
		try {
			tableFacade.readOneRowFromTableUsingConditions("someTableName", conditions);
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testGeneratedSqlQueryForOneString() throws Exception {
		conditions.add("alpha2code", "SE");

		Row result = tableFacade.readOneRowFromTableUsingConditions("someTableName", conditions);
		assertTrue(databaseFacadeSpy.readOneRowFromDbUsingTableAndConditionsWasCalled);

		assertEquals(databaseFacadeSpy.sql, "select * from someTableName where alpha2code = ?");
		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "SE");

		assertSame(result, databaseFacadeSpy.oneRowResult);

	}

	@Test
	public void testGeneratedSqlQueryForOneStringTwoConditions() throws Exception {
		conditions.add("alpha2code", "SE");
		conditions.add("alpha3code", "SWE");
		Row result = tableFacade.readOneRowFromTableUsingConditions("someTableName", conditions);

		assertEquals(databaseFacadeSpy.sql,
				"select * from someTableName where alpha2code = ? and alpha3code = ?");
		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 2);
		assertEquals(values.get(0), "SE");
		assertEquals(values.get(1), "SWE");
		assertSame(result, databaseFacadeSpy.oneRowResult);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadOneRowFromDbUsingTableAndConditionsThrowError() throws Exception {

		databaseFacadeSpy.throwError = true;

		tableFacade.readOneRowFromTableUsingConditions("someTableName", conditions);

	}

	@Test
	public void ReadOneRowFromDbUsingTableAndConditionsWithEmptyConditions() throws Exception {

		Conditions emptyCondtions = new ConditionsImp();
		tableFacade.readOneRowFromTableUsingConditions("someTableName", emptyCondtions);

		assertEquals(databaseFacadeSpy.sql, "select * from someTableName");
		assertEquals(databaseFacadeSpy.values, Collections.emptyList());
	}

	@Test
	public void testReadFromTableUsingConditionReturnsResultFromDataReader() throws Exception {
		conditions.add("alpha2code", "SE");

		List<Row> result = tableFacade.readRowsFromTableUsingConditions("someTableName",
				conditions);

		assertTrue(databaseFacadeSpy.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select * from someTableName where alpha2code = ?");

		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "SE");

		assertSame(result, databaseFacadeSpy.result);
	}

	@Test
	public void testReadFromTableUsingTwoConditionReturnsResultFromDataReader() throws Exception {
		conditions.add("alpha2code", "SE");
		conditions.add("alpha3code", "SWE");

		List<Row> result = tableFacade.readRowsFromTableUsingConditions("someTableName",
				conditions);

		assertTrue(databaseFacadeSpy.executePreparedStatementQueryUsingSqlAndValuesWasCalled);
		assertEquals(databaseFacadeSpy.sql,
				"select * from someTableName where alpha2code = ? and alpha3code = ?");

		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 2);
		assertEquals(values.get(0), "SE");
		assertEquals(values.get(1), "SWE");

		assertSame(result, databaseFacadeSpy.result);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Error reading data from someTableName")
	public void testReadFromTableUsingConditionSqlErrorThrowsError() throws Exception {
		databaseFacadeSpy.throwError = true;
		tableFacade = TableFacadeImp.usingDataReader(databaseFacadeSpy);
		tableFacade.readRowsFromTableUsingConditions("someTableName", conditions);
	}

	@Test(expectedExceptions = SqlDatabaseException.class)
	public void testReadNextFromSequenceError() throws Exception {
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

	@Test
	public void testReadNumberOfRows() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoSpy queryInfo = new DbQueryInfoSpy();
		queryInfo.delimiterIsPresentValue = false;
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertTrue(databaseFacadeSpy.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select count(*) from organisation where domain = ?");
		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "uu");
		assertEquals(numberOfRows, databaseFacadeSpy.oneRowResult.getValueByColumn("count"));

		assertFalse(queryInfo.getToNoWasCalled);
		assertFalse(queryInfo.getFromNoWasCalled);
	}

	@Test
	public void testReadNumberOfRowsNoConditions() {
		String type = "organisation";

		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertTrue(databaseFacadeSpy.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select count(*) from organisation");
		assertTrue(databaseFacadeSpy.values.isEmpty());
		assertEquals(numberOfRows, databaseFacadeSpy.oneRowResult.getValueByColumn("count"));
	}

	@Test
	public void testReadNumberOfRowsWithFromAndTo() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(2, 11);

		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertTrue(databaseFacadeSpy.readOneRowFromDbUsingTableAndConditionsWasCalled);
		assertEquals(databaseFacadeSpy.sql, "select count(*) from organisation where domain = ?");
		List<Object> values = databaseFacadeSpy.values;
		assertEquals(values.size(), 1);
		assertEquals(values.get(0), "uu");
		assertEquals(numberOfRows, 10);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenToLargerThanNumOfRows() {
		String type = "organisation";
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(440, 476);

		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 14);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenFromLargerThanNumOfRows() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(460, 476);

		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 0);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenFromLargerThanTo() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(300, 150);

		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 0);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenFromAndToIsSameAndMax() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(453, 453);
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 1);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenToIsNull() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(400, null);
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 54);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenFromIsZeroUseOneAsFrom() {
		String type = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(0, 10);
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(type,
				conditions, queryInfo);

		assertEquals(numberOfRows, 10);

	}

	@Test
	public void testReadNumberOfRowsWithFromAndToWhenFromIsNullUseOneAsFrom() {
		String tableName = "organisation";
		// Map<String, Object> conditions = new HashMap<>();
		conditions.add("domain", "uu");

		DbQueryInfoImp queryInfo = new DbQueryInfoImp(null, 10);
		long numberOfRows = tableFacade.numberOfRowsInTableForConditionsAndQueryInfo(tableName,
				conditions, queryInfo);

		assertEquals(numberOfRows, 10);

	}

	@Test
	public void testDeleteOneRecordOneCondition() {
		conditions.add("organisation_id", 234);

		tableFacade.deleteRowFromTableUsingConditions("organisation", conditions);

		List<Object> values = getValuesFromExecuteSqlWithValues();

		String deleteSql = "delete from organisation where organisation_id = ?";
		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", deleteSql);
		assertEquals(values.get(0), 234);
	}

	private List<Object> getValuesFromExecuteSqlWithValues() {
		return (List<Object>) databaseFacadeSpy.MCR
				.getValueForMethodNameAndCallNumberAndParameterName("executeSqlWithValues", 0,
						"values");
	}

	@Test
	public void testDeleteOneRecordTwoConditions() {
		conditions.add("organisation_id", 234);
		conditions.add("organisation_name", "someNewOrganisationName");

		tableFacade.deleteRowFromTableUsingConditions("organisation", conditions);

		String deleteSql = "delete from organisation where organisation_id = ? and organisation_name = ?";
		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", deleteSql);

		List<Object> values = getValuesFromExecuteSqlWithValues();

		assertEquals(values.get(0), 234);
		assertEquals(values.get(1), "someNewOrganisationName");
	}

	@Test
	public void testInsertOneRecord() {

		values.put("organisation_id", 234);
		values.put("organisation_name", "someNewOrganisationName");

		tableFacade.insertRowInTableWithValues("organisation", values);
		String insertSql = "insert into organisation(organisation_id, organisation_name) values(?, ?)";
		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", insertSql);

		List<Object> valuesSentToExecutor = getValuesFromExecuteSqlWithValues();

		assertEquals(valuesSentToExecutor.get(0), 234);
		assertEquals(valuesSentToExecutor.get(1), "someNewOrganisationName");
	}

	@Test
	public void testUpdateOneRecordOneColumnOneCondition() {
		values.put("organisation_name", "someNewOrganisationName");
		conditions.add("organisation_id", 123);

		tableFacade.updateRowInTableUsingValuesAndConditions("organisation", values, conditions);

		String updateSql = "update organisation set organisation_name = ? where organisation_id = ?";
		List<Object> valuesSentToExecutor = getValuesFromExecuteSqlWithValues();

		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", updateSql);
		assertEquals(valuesSentToExecutor.get(0), "someNewOrganisationName");
		assertEquals(valuesSentToExecutor.get(1), 123);
	}

	@Test
	public void testUpdateOneRecordTwoColumnsOneCondition() {
		values.put("organisation_name", "someNewOrganisationName");
		values.put("organisation_code", "someNewOrgCode");
		conditions.add("organisation_id", 123);

		tableFacade.updateRowInTableUsingValuesAndConditions("organisation", values, conditions);

		String updateSql = "update organisation set organisation_code = ?, organisation_name = ? where organisation_id = ?";
		List<Object> valuesSentToExecutor = getValuesFromExecuteSqlWithValues();

		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", updateSql);
		assertEquals(valuesSentToExecutor.get(0), "someNewOrgCode");
		assertEquals(valuesSentToExecutor.get(1), "someNewOrganisationName");
		assertEquals(valuesSentToExecutor.get(2), 123);
	}

	@Test
	public void testUpdateOneRecordTwoColumnsTwoConditions() {
		values.put("organisation_name", "someNewOrganisationName");
		values.put("organisation_code", "someNewOrgCode");
		conditions.add("organisation_id", 123);
		conditions.add("country_code", "swe");

		tableFacade.updateRowInTableUsingValuesAndConditions("organisation", values, conditions);

		String updateSql = "update organisation set organisation_code = ?, organisation_name = ? where organisation_id = ? and country_code = ?";
		List<Object> valuesSentToExecutor = getValuesFromExecuteSqlWithValues();

		databaseFacadeSpy.MCR.assertParameter("executeSqlWithValues", 0, "sql", updateSql);
		assertEquals(valuesSentToExecutor.get(0), "someNewOrgCode");
		assertEquals(valuesSentToExecutor.get(1), "someNewOrganisationName");
		assertEquals(valuesSentToExecutor.get(2), 123);
		assertEquals(valuesSentToExecutor.get(3), "swe");
	}

}