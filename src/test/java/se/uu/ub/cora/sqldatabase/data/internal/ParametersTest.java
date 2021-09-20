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
package se.uu.ub.cora.sqldatabase.data.internal;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.DatabaseNull;
import se.uu.ub.cora.sqldatabase.Parameters;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.internal.ParametersImp;

public class ParametersTest {

	Parameters parameters;
	private static final String SOME_PARAMETER = "someParameter";

	@BeforeMethod
	public void beforeMethod() {
		parameters = new ParametersImp();
	}

	@Test
	public void testAddParameter() throws Exception {

		Object parameterValue = "parameterValue";
		parameters.add(SOME_PARAMETER, parameterValue);

		Object value = parameters.getValue(SOME_PARAMETER);
		assertEquals(value, parameterValue);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Parameter\\: NoExistingParameter, does not exist")
	public void testGetValueForNoExistingCondition() throws Exception {

		parameters.getValue("NoExistingParameter");
	}

	@Test
	public void testAddConditionWithDatabaseNullValue() throws Exception {
		parameters.add(SOME_PARAMETER, new DatabaseNull());

		Object value = parameters.getValue(SOME_PARAMETER);

		assertTrue(value instanceof DatabaseNull);
	}

	@Test
	public void testGetNames() throws Exception {
		addParametersAndValues();

		List<String> myList = parameters.getNames();

		assertEquals(myList.size(), 3);
		assertEquals(myList.get(0), "someParameter1");
		assertEquals(myList.get(1), "someParameter2");
		assertEquals(myList.get(2), "someParameter3");
	}

	@Test
	public void testGetValues() throws Exception {
		addParametersAndValues();

		List<Object> myList = parameters.getValues();

		assertEquals(myList.size(), 3);
		assertEquals(myList.get(0), "someValue1");
		assertEquals(myList.get(1), "someValue2");
		assertEquals(myList.get(2), "someValue3");
	}

	private void addParametersAndValues() {
		parameters.add("someParameter1", "someValue1");
		parameters.add("someParameter2", "someValue2");
		parameters.add("someParameter3", "someValue3");
	}

	@Test
	public void testHasParameters() throws Exception {
		assertFalse(parameters.hasParameters());
	}

	@Test
	public void testHasParametersOneIsAdded() throws Exception {
		parameters.add("someParameter1", "someValue1");
		assertTrue(parameters.hasParameters());

	}

}
