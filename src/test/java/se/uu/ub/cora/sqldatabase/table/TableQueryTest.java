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
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;

public class TableQueryTest {

	String tableName = "someTableName";

	TableQuery tableQuery;

	@BeforeMethod
	public void beforeMethod() {
		tableQuery = TableQueryImp.usingTableName(tableName);
	}

	@Test
	public void testInitTableQueryImp() throws Exception {
		assertTrue(tableQuery instanceof TableQuery);
	}

	@Test
	public void testCreateSqlOneParameter() throws Exception {
		tableQuery.addParameter("parameterName1", "parameterValue1");
		assertEquals(tableQuery.assembleCreateSql(),
				"insert into " + tableName + "(parameterName1) values(?)");
		assertQueryValues("parameterValue1");
	}

	@Test
	public void testCreateSqlTwoParameters() throws Exception {
		tableQuery.addParameter("parameterName1", "parameterValue1");
		tableQuery.addParameter("parameterName2", "parameterValue2");
		assertEquals(tableQuery.assembleCreateSql(),
				"insert into " + tableName + "(parameterName1, parameterName2) values(?, ?)");
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
		tableQuery.addCondition("conditionName1", "conditionValue1");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " where conditionName1 = ?");
		assertQueryValues("conditionValue1");
	}

	@Test
	public void testReadSqlWithTwoConditions() throws Exception {
		tableQuery.addCondition("conditionName1", "conditionValue1");
		tableQuery.addCondition("conditionName2", "conditionValue2");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " where conditionName1 = ? and conditionName2 = ?");
		assertQueryValues("conditionValue1", "conditionValue2");
	}
	// setOrderBy(String orderBy);
	// setSortOrder(SortOrder sortOrder);
	// setFromNo(Integer fromNo);
	// setToNo(Integer toNo);

	@Test
	public void testReadSqlWithOneOrderBy() throws Exception {
		tableQuery.addOrderByAsc("column1");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by column1 asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderBy() throws Exception {
		tableQuery.addOrderByAsc("column1");
		tableQuery.addOrderByAsc("column2");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by column1 asc, column2 asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderByDescAsc() throws Exception {
		tableQuery.addOrderByDesc("column1");
		tableQuery.addOrderByAsc("column2");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by column1 desc, column2 asc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testReadSqlWithTwoOrderByAscDesc() throws Exception {
		tableQuery.addOrderByAsc("column1");
		tableQuery.addOrderByDesc("column2");
		assertEquals(tableQuery.assembleReadSql(),
				"select * from " + tableName + " order by column1 asc, column2 desc");
		assertTrue(tableQuery.getQueryValues().isEmpty());
	}

	@Test
	public void testUpdateSqlOneParameter() throws Exception {
		tableQuery.addParameter("parameterName1", "parameterValue1");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterName1 = ?");
		assertQueryValues("parameterValue1");
	}

	@Test
	public void testUpdateSqlTwoParameters() throws Exception {
		tableQuery.addParameter("parameterName1", "parameterValue1");
		tableQuery.addParameter("parameterName2", "parameterValue2");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterName1 = ?, parameterName2 = ?");
		assertQueryValues("parameterValue1", "parameterValue2");
	}

	@Test
	public void testUpdateSqlTwoParametersTwoConditions() throws Exception {
		tableQuery.addParameter("parameterName1", "parameterValue1");
		tableQuery.addParameter("parameterName2", "parameterValue2");
		tableQuery.addCondition("conditionName1", "conditionValue1");
		tableQuery.addCondition("conditionName2", "conditionValue2");
		assertEquals(tableQuery.assembleUpdateSql(),
				"update " + tableName + " set parameterName1 = ?, parameterName2 = ?"
						+ " where conditionName1 = ? and conditionName2 = ?");
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
		tableQuery.addCondition("conditionName1", "conditionValue1");
		tableQuery.addCondition("conditionName2", "conditionValue2");
		assertEquals(tableQuery.assembleDeleteSql(),
				"delete from " + tableName + " where conditionName1 = ? and conditionName2 = ?");
		assertQueryValues("conditionValue1", "conditionValue2");
	}

}
