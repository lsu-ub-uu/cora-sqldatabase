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

import se.uu.ub.cora.sqldatabase.Conditions;
import se.uu.ub.cora.sqldatabase.DatabaseNull;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.internal.ConditionsImp;

public class ConditionsTest {

	private Conditions conditions;

	@BeforeMethod
	void beforeMethod() {
		conditions = new ConditionsImp();
	}

	@Test
	public void testAddCondition() throws Exception {
		String conditionName = "aCondition";

		Object condtionValue = "condtionValue";
		conditions.add(conditionName, condtionValue);

		Object value = conditions.getValue(conditionName);
		assertEquals(value, condtionValue);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Condition\\: NoExistingCondition, does not exist")
	public void testGetValueForNoExistingCondition() throws Exception {

		conditions.getValue("NoExistingCondition");
	}

	@Test
	public void testAddConditionWithDatabaseNullValue() throws Exception {
		conditions.add("someCondition", new DatabaseNull());

		Object value = conditions.getValue("someCondition");

		assertTrue(value instanceof DatabaseNull);
	}

	@Test
	public void testGetNames() throws Exception {
		conditions.add("someCondition1", "someValue1");
		conditions.add("someCondition2", "someValue2");
		conditions.add("someCondition3", "someValue3");

		List<String> myList = conditions.getNames();

		assertEquals(myList.size(), 3);
		assertEquals(myList.get(0), "someCondition1");
		assertEquals(myList.get(1), "someCondition2");
		assertEquals(myList.get(2), "someCondition3");
	}

	@Test
	public void testGetValues() throws Exception {
		conditions.add("someCondition1", "someValue1");
		conditions.add("someCondition2", "someValue2");
		conditions.add("someCondition3", "someValue3");

		List<Object> myList = conditions.getValues();

		assertEquals(myList.size(), 3);
		assertEquals(myList.get(0), "someValue1");
		assertEquals(myList.get(1), "someValue2");
		assertEquals(myList.get(2), "someValue3");
	}

	@Test
	public void testHasConditions() throws Exception {
		assertFalse(conditions.hasConditions());
	}

	@Test
	public void testHasConditionsOneIsAdded() throws Exception {
		conditions.add("someCondition1", "someValue1");
		assertTrue(conditions.hasConditions());

	}

}
