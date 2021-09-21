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

import se.uu.ub.cora.sqldatabase.table.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.table.OrderCriteria;

public class DbQueryInfoSpy implements DbQueryInfo {

	public boolean delimiterIsPresentValue = true;
	public boolean getToNoWasCalled = false;
	public boolean getFromNoWasCalled = false;

	public DbQueryInfoSpy(int fromNo, int toNo) {
		// TODO Auto-generated constructor stub
	}

	public DbQueryInfoSpy() {
		// TODO Auto-generated constructor stub
	}

	@Override
	public Integer getOffset() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Integer getLimit() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDelimiter() {
		return " delimiter from spy";
	}

	@Override
	public Integer getFromNo() {
		getFromNoWasCalled = true;
		return null;
	}

	@Override
	public Integer getToNo() {
		getToNoWasCalled = true;
		return null;
	}

	@Override
	public boolean delimiterIsPresent() {
		return delimiterIsPresentValue;
	}

	@Override
	public void setOrderBy(String orderBy) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getOrderByPartOfQuery() {
		// TODO Auto-generated method stub
		return " order by from spy";
	}

	public void setSortOrder(OrderCriteria asc) {
		// TODO Auto-generated method stub

	}

}
