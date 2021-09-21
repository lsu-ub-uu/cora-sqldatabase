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
package se.uu.ub.cora.sqldatabase.table.internal;

import se.uu.ub.cora.sqldatabase.table.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.table.OrderCriteria;

public class DbQueryInfoImp implements DbQueryInfo {
	private static final int MIN_OFFSET = 0;
	private Integer fromNo;
	private Integer toNo;
	private String orderBy;
	private OrderCriteria sortOrder;

	public DbQueryInfoImp() {
	}

	public DbQueryInfoImp(Integer fromNo, Integer toNo) {
		this.fromNo = fromNo;
		this.toNo = toNo;
	}

	@Override
	public Integer getOffset() {
		if (!fromNoIsPresent()) {
			return null;
		}
		return fromNo > MIN_OFFSET ? fromNo - 1 : MIN_OFFSET;
	}

	@Override
	public Integer getLimit() {
		if (!toNoIsPresent()) {
			return null;
		}
		return fromNoIsPresent() ? fromToDifferencePlusOne(fromNo, toNo) : toNo;
	}

	private boolean fromNoIsPresent() {
		return fromNo != null;
	}

	private int fromToDifferencePlusOne(Integer fromNo, Integer toNo) {
		return (toNo - fromNo) + 1;
	}

	@Override
	public String getDelimiter() {
		String limiQueryPart = getLimitPartOfQuery();
		String offsetQueryPart = getOffsetPartOfQuery();
		return limiQueryPart + offsetQueryPart;
	}

	private String getLimitPartOfQuery() {
		Integer limit = getLimit();
		return limit != null ? " limit " + limit : "";
	}

	private String getOffsetPartOfQuery() {
		Integer offset = getOffset();
		return offset != null ? " offset " + offset : "";
	}

	@Override
	public Integer getFromNo() {
		return fromNo;
	}

	@Override
	public Integer getToNo() {
		return toNo;
	}

	@Override
	public boolean delimiterIsPresent() {
		return fromNoIsPresent() || toNoIsPresent();
	}

	private boolean toNoIsPresent() {
		return toNo != null;
	}

	@Override
	public void setOrderBy(String orderBy) {
		this.orderBy = orderBy;

	}

	@Override
	public String getOrderByPartOfQuery() {
		String orderByPart = valueForOrderByExists() ? " order by " + orderBy : "";
		orderByPart += possiblyAddSortOrderPart();
		return orderByPart;
	}

	private String possiblyAddSortOrderPart() {
		if (sortOrder != null) {
			return " " + sortOrder.order;
		}
		return "";
	}

	private boolean valueForOrderByExists() {
		return orderBy != null && !orderBy.isEmpty();
	}

	@Override
	public void setSortOrder(OrderCriteria sortOrder) {
		this.sortOrder = sortOrder;
	}

}
