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
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class DbQueryInfoTest {

	@Test
	public void testDefaultOffsetDefaultLimit() {
		DbQueryInfo queryInfo = new DbQueryInfo();
		assertNull(queryInfo.getOffset());
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), "");
	}

	@Test
	public void testOffset() {
		DbQueryInfo queryInfo = new DbQueryInfo(3, null);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(2));
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), " offset 2");
	}

	@Test
	public void testOffsetWhenFromNoIsZero() {
		DbQueryInfo queryInfo = new DbQueryInfo(0, null);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(0));
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), " offset 0");
	}

	@Test
	public void testLimitWhenNoFromNoButToNo() {
		DbQueryInfo queryInfo = new DbQueryInfo(null, 67);
		assertEquals(queryInfo.getLimit(), Integer.valueOf(67));
		assertNull(queryInfo.getOffset());
		assertEquals(queryInfo.getDelimiter(), " limit 67");
	}

	@Test
	public void testOffsetAndLimit() {
		DbQueryInfo queryInfo = new DbQueryInfo(8, 19);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(7));
		assertEquals(queryInfo.getLimit(), Integer.valueOf(12));
		assertEquals(queryInfo.getDelimiter(), " limit 12 offset 7");
	}

}
