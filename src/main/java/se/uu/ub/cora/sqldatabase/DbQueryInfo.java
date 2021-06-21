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

public class DbQueryInfo {
	private static final int MIN_OFFSET = 0;
	private Integer fromNo;
	private Integer toNo;

	public DbQueryInfo() {
	}

	public DbQueryInfo(Integer fromNo, Integer toNo) {
		this.fromNo = fromNo;
		this.toNo = toNo;
	}

	public Integer getOffset() {
		if (fromNo == null) {
			return null;
		}
		return fromNo > MIN_OFFSET ? fromNo - 1 : MIN_OFFSET;
	}

	public Integer getLimit() {
		if (toNo == null) {
			return null;
		}
		return fromNo != null ? fromToDifferencePlusOne(fromNo, toNo) : toNo;
	}

	private int fromToDifferencePlusOne(int fromNo, int toNo) {
		return (toNo - fromNo) + 1;
	}

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

}
