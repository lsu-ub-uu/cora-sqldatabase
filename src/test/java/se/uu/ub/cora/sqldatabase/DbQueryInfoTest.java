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
import static org.testng.Assert.assertNull;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.Test;

public class DbQueryInfoTest {

	@Test
	public void testGetFromAndTo() {
		DbQueryInfo queryInfo = new DbQueryInfoImp(2, 78);
		assertEquals(queryInfo.getFromNo(), Integer.valueOf(2));
		assertEquals(queryInfo.getToNo(), Integer.valueOf(78));

	}

	@Test
	public void testDefaultOffsetDefaultLimit() {
		DbQueryInfo queryInfo = new DbQueryInfoImp();
		assertNull(queryInfo.getOffset());
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), "");
		assertFalse(queryInfo.delimiterIsPresent());
	}

	@Test
	public void testOffset() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(3, null);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(2));
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), " offset 2");
	}

	@Test
	public void testOffsetWhenFromNoIsZero() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(0, null);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(0));
		assertNull(queryInfo.getLimit());
		assertEquals(queryInfo.getDelimiter(), " offset 0");
	}

	@Test
	public void testLimitWhenNoFromNoButToNo() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(null, 67);
		assertEquals(queryInfo.getLimit(), Integer.valueOf(67));
		assertNull(queryInfo.getOffset());
		assertEquals(queryInfo.getDelimiter(), " limit 67");
	}

	@Test
	public void testOffsetAndLimit() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(8, 19);
		assertEquals(queryInfo.getOffset(), Integer.valueOf(7));
		assertEquals(queryInfo.getLimit(), Integer.valueOf(12));
		assertEquals(queryInfo.getDelimiter(), " limit 12 offset 7");
	}

	@Test
	public void testDelimiterIsPresentBothIsNull() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		assertFalse(queryInfo.delimiterIsPresent());
	}

	@Test
	public void testDelimiterIsPresentFromNoIsPresent() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(4, null);
		assertTrue(queryInfo.delimiterIsPresent());
	}

	@Test
	public void testDelimiterIsPresentToNoIsPresent() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(null, 45);
		assertTrue(queryInfo.delimiterIsPresent());
	}

	@Test
	public void testDelimiterIsPresentBothArePresent() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp(5, 45);
		assertTrue(queryInfo.delimiterIsPresent());
	}

	@Test
	public void testOrderByPartOfQuery() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		queryInfo.setOrderBy("organisation_id");
		assertEquals(queryInfo.getOrderByPartOfQuery(), " order by " + "organisation_id");
	}

	@Test
	public void testOrderByPartOfQueryWhenOrderByIsNull() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		assertEquals(queryInfo.getOrderByPartOfQuery(), "");
	}

	@Test
	public void testOrderByPartOfQueryWhenOrderByIsEmpty() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		queryInfo.setOrderBy("");
		assertEquals(queryInfo.getOrderByPartOfQuery(), "");
	}

	@Test
	public void testOrderByPartOfQueryWithSortOrder() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		queryInfo.setOrderBy("organisation_id");

		queryInfo.setSortOrder(SortOrder.ASC);
		assertEquals(queryInfo.getOrderByPartOfQuery(),
				" order by " + "organisation_id" + " " + SortOrder.ASC.order);
	}

	@Test
	public void testOrderByPartOfQueryWithSortOrderDesc() {
		DbQueryInfoImp queryInfo = new DbQueryInfoImp();
		queryInfo.setOrderBy("organisation_id");

		queryInfo.setSortOrder(SortOrder.DESC);
		assertEquals(queryInfo.getOrderByPartOfQuery(),
				" order by " + "organisation_id" + " " + SortOrder.DESC.order);
	}

}
