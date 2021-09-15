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
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.data.DatabaseNull;
import se.uu.ub.cora.sqldatabase.data.Row;

public class RowTest {

	private Row row;

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

		row.addColumnWithValue("aColumn1", "anObject1");
		row.addColumnWithValue("aColumn2", "anObject2");
		row.addColumnWithValue("aColumn3", "anObject3");

		assertEquals(row.getValueByColumn("aColumn1"), "anObject1");
		assertEquals(row.getValueByColumn("aColumn2"), "anObject2");
		assertEquals(row.getValueByColumn("aColumn3"), "anObject3");
	}

	@Test
	public void testAddColumnWithNullValue() throws Exception {
		row.addColumnWithValue("aColumn", null);

		Object valueFromColumn = row.getValueByColumn("aColumn");

		assertTrue(valueFromColumn instanceof DatabaseNull);
	}

	@Test(expectedExceptions = SqlDatabaseException.class, expectedExceptionsMessageRegExp = ""
			+ "Column\\: NoExistingColumn, does not exist")
	public void testGetValueByColumnWitNoExistingColumnName() throws Exception {

		row.getValueByColumn("NoExistingColumn");
	}

}
