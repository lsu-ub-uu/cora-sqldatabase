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
package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertFalse;
import static org.testng.Assert.assertTrue;

import java.util.Set;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.internal.RowImp;

public class RowTest {

	private RowImp row;

	@BeforeMethod
	void beforeMethod() {
		row = new RowImp();
	}

	@Test
	public void testAddColumnWithValues() throws Exception {

		row.addColumnWithValue("aColumn", "anObject");

		Object valueFromColumn = row.getValueByColumn("aColumn");
		assertEquals(valueFromColumn, "anObject");
	}

	@Test
	public void testAddSeveralColumnsWithValues() throws Exception {
		addThreeColumnsWithValues();

		assertEquals(row.getValueByColumn("aColumn1"), "anObject1");
		assertEquals(row.getValueByColumn("aColumn2"), "anObject2");
		assertEquals(row.getValueByColumn("aColumn3"), "anObject3");
	}

	private void addThreeColumnsWithValues() {
		row.addColumnWithValue("aColumn1", "anObject1");
		row.addColumnWithValue("aColumn2", "anObject2");
		row.addColumnWithValue("aColumn3", "anObject3");
	}

	@Test
	public void testAddColumnWithNullValue() throws Exception {
		row.addColumnWithValue("aColumn", null);

		Object valueFromColumn = row.getValueByColumn("aColumn");

		assertTrue(DatabaseValues.NULL == valueFromColumn);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Column\\: NoExistingColumn, does not exist")
	public void testGetValueByColumnWitNoExistingColumnName() throws Exception {

		row.getValueByColumn("NoExistingColumn");
	}

	@Test
	public void testColumnsSet() throws Exception {
		addThreeColumnsWithValues();

		Set<String> columnSet = row.columnSet();
		assertEquals(columnSet.size(), 3);
	}

	@Test
	public void testHasColumn() throws Exception {
		addThreeColumnsWithValues();

		assertFalse(row.hasColumn("nonExistingColumnName"));
		assertTrue(row.hasColumn("aColumn1"));
	}

	@Test
	public void testHasColumnWithNonEmptyValue() throws Exception {
		addFourColumnsForEmptyValuesTest();
		assertFalse(row.hasColumnWithNonEmptyValue("nonExistingColumnName"));
		assertFalse(row.hasColumnWithNonEmptyValue("aColumn1"));
		assertFalse(row.hasColumnWithNonEmptyValue("aColumn2"));
		assertFalse(row.hasColumnWithNonEmptyValue("aColumn3"));

		assertTrue(row.hasColumnWithNonEmptyValue("aColumn4"));
		assertTrue(row.hasColumnWithNonEmptyValue("aColumn5"));
		assertTrue(row.hasColumnWithNonEmptyValue("aColumn6"));
	}

	private void addFourColumnsForEmptyValuesTest() {
		row.addColumnWithValue("aColumn1", "");
		row.addColumnWithValue("aColumn2", "  ");
		row.addColumnWithValue("aColumn3", DatabaseValues.NULL);
		row.addColumnWithValue("aColumn4", "someValue");
		row.addColumnWithValue("aColumn5", 1);
		row.addColumnWithValue("aColumn6", 2L);
	}
}
