/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.table;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;

public class TableQueryTest {

	List<String> textsWithForbiddenCharacters = new ArrayList<>();
	List<String> textsWithAllowedCharacters = new ArrayList<>();
	String tableName = "someTableName";

	TableQuery tableQuery;

	TableQueryTest() {
		textsWithForbiddenCharacters.add("(select * from person) as person");
		textsWithForbiddenCharacters.add("table Name");
		textsWithForbiddenCharacters.add("table where 1=1");
		textsWithForbiddenCharacters.add("table=Name");
		textsWithForbiddenCharacters.add("table\"Name");
		textsWithForbiddenCharacters.add("table*Name");
		textsWithForbiddenCharacters.add("table\\Name");
		textsWithForbiddenCharacters.add("table+Name");
		textsWithForbiddenCharacters.add("table+Name");
		textsWithForbiddenCharacters.add("table\\u0054Name");
		textsWithForbiddenCharacters.add("DROP\stableName");
		textsWithForbiddenCharacters.add("table\\Name");

		textsWithAllowedCharacters.add("tableName");
		textsWithAllowedCharacters.add("TABLENAME");
		textsWithAllowedCharacters.add("tablename");
		textsWithAllowedCharacters.add("table-Name");
		textsWithAllowedCharacters.add("table_Name");
		textsWithAllowedCharacters.add("table.Name");
	}

	@BeforeMethod
	public void beforeMethod() {
		tableQuery = TableQueryImp.usingTableName(tableName);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Input contains character outside the allowed regexp.")
	public void testTableNameContainsUncommonLetters() throws Exception {
		TableQueryImp.usingTableName("table'Name");
	}

	@Test
	public void testConstructorFindsForbiddenCharacters() throws Exception {
		for (String text : textsWithForbiddenCharacters) {
			assertConstructorHandlesForbiddenCharacters(text);
		}
	}

	private void assertConstructorHandlesForbiddenCharacters(String name) {
		try {
			TableQueryImp.usingTableName(name);
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testOkTableNames() throws Exception {
		for (String text : textsWithAllowedCharacters) {
			TableQueryImp.usingTableName(text);
		}
		assertTrue(true);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Input contains character outside the allowed regexp.")
	public void testAddParameterNameContainsUncommonLetters() throws Exception {
		tableQuery.addParameter("some'Paramter", "someValue");
	}

	@Test
	public void testAddParameterFindsForbiddenCharacters() throws Exception {
		for (String text : textsWithForbiddenCharacters) {
			assertAddParameterHandlesForbiddenCharacters(text);
		}
	}

	private void assertAddParameterHandlesForbiddenCharacters(String name) {
		try {
			tableQuery.addParameter(name, "someValue");
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testAddParameterNames() throws Exception {
		for (String text : textsWithAllowedCharacters) {
			tableQuery.addParameter(text, "someValue");
		}
		assertTrue(true);
	}

	@Test
	public void testAddConditionFindsForbiddenCharacters() throws Exception {
		for (String text : textsWithForbiddenCharacters) {
			assertAddConditionsHandlesForbiddenCharacters(text);
		}
	}

	private void assertAddConditionsHandlesForbiddenCharacters(String name) {
		try {
			tableQuery.addCondition(name, "someValue");
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testAddConditionNames() throws Exception {
		for (String text : textsWithAllowedCharacters) {
			tableQuery.addCondition(text, "someValue");
		}
		assertTrue(true);
	}

	@Test
	public void testAddOrderByFindsForbiddenCharacters() throws Exception {
		for (String text : textsWithForbiddenCharacters) {
			assertAddOrderBysHandlesForbiddenCharacters(text, "asc");
			assertAddOrderBysHandlesForbiddenCharacters(text, "desc");
		}
	}

	private void assertAddOrderBysHandlesForbiddenCharacters(String name, String orderDirection) {
		try {
			if ("asc".equals(orderDirection))
				tableQuery.addOrderByAsc(name);
			else
				tableQuery.addOrderByDesc(name);
			assertFalse(true);
		} catch (Exception e) {
			assertTrue(true);
		}
	}

	@Test
	public void testAddOrderByNames() throws Exception {
		for (String text : textsWithAllowedCharacters) {
			tableQuery.addOrderByAsc(text);
			tableQuery.addOrderByDesc(text);
		}
		assertTrue(true);
	}

	@Test
	public void testInitTableQueryImp() throws Exception {
		assertTrue(tableQuery instanceof TableQuery);
	}

	@Test
	public void testCreateSqlOneParameter() throws Exception {
		tableQuery.addParameter("parameterNameA", "parameterValue1");
		assertEquals(tableQuery.assembleCreateSql(),
				"insert into " + tableName + "(parameterNameA) values(?)");
		assertQueryValues("parameterValue1");
	}

	@Test
	public void testCreateSqlTwoParameters() throws Exception {
		tableQuery.addParameter("parameterNameA", "parameterValue1");
		tableQuery.addParameter("parameterNameB", "parameterValue2");
		assertEquals(tableQuery.assembleCreateSql(),
				"insert into " + tableName + "(parameterNameA, parameterNameB) values(?, ?)");
		assertQueryValues("parameterValue1", "parameterValue2");
	}

	private void assertQueryValues(Object... expectedValues) {
		List<Object> values = tableQuery.getQueryValues();
		int i = 0;
		for (Object object : expectedValues) {
			assertEquals(values.get(i), object);
			i++;
		}
	}

	@Test
	public void testReadSql() throws Exception {
		assertEquals(tableQuery.assembleReadSql(), "select * from " + tableName);
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithOneCondition() throws Exception {
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " where conditionNameA = ?");
		assertQueryValues("conditionValue1");
	}

	@Test
	public void testReadSqlWithTwoConditions() throws Exception {
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		tableQuery.addCondition("conditionNameB", "conditionValue2");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " where conditionNameA = ? and conditionNameB = ?");
		assertQueryValues("conditionValue1", "conditionValue2");
	}

	@Test
	public void testReadSqlWithFromNo() throws Exception {
		tableQuery.setFromNo(1L);
		assertEquals(tableQuery.assembleReadSql(), "select * from " + tableName + " offset 0");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithFromNo10() throws Exception {
		tableQuery.setFromNo(10L);
		assertEquals(tableQuery.assembleReadSql(), "select * from " + tableName + " offset 9");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithToNo() throws Exception {
		tableQuery.setToNo(10L);
		assertEquals(tableQuery.assembleReadSql(), "select * from " + tableName + " limit 10");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithFromNoAndToNo() throws Exception {
		tableQuery.setFromNo(10L);
		tableQuery.setToNo(20L);
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " offset 9 limit 10");
	}

	@Test
	public void testReadSqlWithFromNoAndToNoOrderOfSetUnimportant() throws Exception {
		tableQuery.setToNo(20L);
		tableQuery.setFromNo(10L);
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " offset 9 limit 10");
	}

	@Test
	public void testReadSqlWithOneOrderBy() throws Exception {
		tableQuery.addOrderByAsc("columnA");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by columnA asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderBy() throws Exception {
		tableQuery.addOrderByAsc("columnA");
		tableQuery.addOrderByAsc("columnB");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by columnA asc, columnB asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderByDescAsc() throws Exception {
		tableQuery.addOrderByDesc("columnA");
		tableQuery.addOrderByAsc("columnB");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by columnA desc, columnB asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderByAscDesc() throws Exception {
		tableQuery.addOrderByAsc("columnA");
		tableQuery.addOrderByDesc("columnB");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by columnA asc, columnB desc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithEverything() throws Exception {
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		tableQuery.addCondition("conditionNameB", "conditionValue2");
		tableQuery.setFromNo(10L);
		tableQuery.setToNo(20L);
		tableQuery.addOrderByDesc("columnA");
		tableQuery.addOrderByAsc("columnB");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " where conditionNameA = ? and conditionNameB = ?"
						+ " order by columnA desc, columnB asc offset 9 limit 10");
		assertQueryValues("conditionValue1", "conditionValue2");
	}

	@Test
	public void testCountSql() throws Exception {
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		tableQuery.addCondition("conditionNameB", "conditionValue2");
		tableQuery.setFromNo(10L);
		tableQuery.setToNo(20L);
		assertEquals(tableQuery.assembleCountSql(),
				"select count (*) from (select * from " + tableName
						+ " where conditionNameA = ? and conditionNameB = ?"
						+ " offset 9 limit 10) as count");
		assertQueryValues("conditionValue1", "conditionValue2");
	}

	@Test
	public void testUpdateSqlOneParameter() throws Exception {
		tableQuery.addParameter("parameterNameA", "parameterValue1");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterNameA = ?");
		assertQueryValues("parameterValue1");
	}

	@Test
	public void testUpdateSqlTwoParameters() throws Exception {
		tableQuery.addParameter("parameterNameA", "parameterValue1");
		tableQuery.addParameter("parameterNameB", "parameterValue2");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterNameA = ?, parameterNameB = ?");
		assertQueryValues("parameterValue1", "parameterValue2");
	}

	@Test
	public void testUpdateSqlTwoParametersTwoConditions() throws Exception {
		tableQuery.addParameter("parameterNameA", "parameterValue1");
		tableQuery.addParameter("parameterNameB", "parameterValue2");
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		tableQuery.addCondition("conditionNameB", "conditionValue2");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterNameA = ?, parameterNameB = ?"
						+ " where conditionNameA = ? and conditionNameB = ?");
		assertQueryValues("parameterValue1", "parameterValue2", "conditionValue1",
				"conditionValue2");
	}

	@Test
	public void testDeleteSqlNoConditions() throws Exception {
		assertEquals(tableQuery.assembleDeleteSql(), "delete from " + tableName);
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testDeleteSqlTwoConditions() throws Exception {
		tableQuery.addCondition("conditionNameA", "conditionValue1");
		tableQuery.addCondition("conditionNameB", "conditionValue2");
		assertEquals(tableQuery.assembleDeleteSql(),
				"delete from " + tableName + " where conditionNameA = ? and conditionNameB = ?");
		assertQueryValues("conditionValue1", "conditionValue2");
	}

}
