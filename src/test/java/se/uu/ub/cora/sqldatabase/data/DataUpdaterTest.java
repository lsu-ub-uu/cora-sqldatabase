/*
 * Copyright 2019 Uppsala University Library
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
import static org.testng.Assert.assertTrue;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.SqlConnectionProviderSpy;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.connection.ConnectionSpy;
import se.uu.ub.cora.sqldatabase.connection.PreparedStatementSpy;

public class DataUpdaterTest {

	private SqlConnectionProviderSpy sqlConnectionProviderSpy;
	private List<Object> values;
	private DataUpdater dataReader;
	private String sql = "update testTable set x=? where y = ?";

	@BeforeMethod
	public void beforeMethod() {
		sqlConnectionProviderSpy = new SqlConnectionProviderSpy();
		dataReader = DataUpdaterImp.usingSqlConnectionProvider(sqlConnectionProviderSpy);
		values = new ArrayList<>();
	}

	@Test
	public void testNoAffectedRows() {
		int updatedRows = dataReader.executeUsingSqlAndValues(sql, values);
		assertEquals(updatedRows, 0);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Error executing statement: update testTable set x=\\? where y = \\?")
	public void testExecuteSqlThrowsError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		dataReader.executeUsingSqlAndValues(sql, values);
	}

	@Test
	public void testExecuteSqlErrorThrowsErrorAndSendsAlongOriginalError() throws Exception {
		sqlConnectionProviderSpy.returnErrorConnection = true;
		try {
			dataReader.executeUsingSqlAndValues(sql, values);
		} catch (Exception e) {
			assertEquals(e.getCause().getMessage(), "error thrown from prepareStatement in spy");
		}
	}

	@Test
	public void testSqlSetAsPreparedStatement() throws Exception {
		dataReader.executeUsingSqlAndValues(sql, values);
		String generatedSql = sqlConnectionProviderSpy.connection.sql;
		assertEquals(generatedSql, sql);
	}

	@Test
	public void testExecuteIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executeUsingSqlAndValues(sql, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.executeUpdateWasCalled);
	}

	@Test
	public void testCloseOfConnectionIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executeUsingSqlAndValues(sql, values);
		ConnectionSpy connectionSpy = sqlConnectionProviderSpy.connection;
		assertTrue(connectionSpy.closeWasCalled);
	}

	@Test
	public void testCloseOfPrepareStatementIsCalledForExecutePreparedStatement() throws Exception {
		dataReader.executeUsingSqlAndValues(sql, values);
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		assertTrue(preparedStatementSpy.closeWasCalled);
	}

	@Test
	public void testUsingValuesFromConditionsForExecutePreparedStatement() throws Exception {
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		values.add("SE");
		values.add("SWE");
		dataReader.executeUsingSqlAndValues(sql, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertEquals(preparedStatementSpy.usedSetObjects.get("2"), "SWE");
	}

	@Test
	public void testReturnFromConditionsForExecutePreparedStatement() throws Exception {
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		preparedStatementSpy.noOfAffectedRows = 5;
		values.add("SE");
		values.add("SWE");
		int updatedRows = dataReader.executeUsingSqlAndValues(sql, values);
		assertEquals(updatedRows, 5);
	}

	@Test
	public void testSetTimestampPreparedStatement() throws Exception {
		PreparedStatementSpy preparedStatementSpy = sqlConnectionProviderSpy.connection.preparedStatementSpy;
		values.add("SE");

		Date today = new Date();
		long time = today.getTime();
		Timestamp timestamp = new Timestamp(time);
		values.add(timestamp);
		dataReader.executeUsingSqlAndValues(sql, values);
		assertEquals(preparedStatementSpy.usedSetObjects.get("1"), "SE");
		assertTrue(preparedStatementSpy.usedSetTimestamps.get("2") instanceof Timestamp);
	}
}
