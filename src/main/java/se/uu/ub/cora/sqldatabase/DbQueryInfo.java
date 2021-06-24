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

public interface DbQueryInfo {

	/**
	 * getOffset returns the offset to use in a sql query, based on the fromNo set in the instance.
	 * FromNo is expected to be 1 if the result should start with the first record. Offset however
	 * starts at zero, and will never be lower than zero. If no fromNo is set in the instance, zero
	 * will be returned.
	 * 
	 * @return Integer offset
	 */
	Integer getOffset();

	/**
	 * getLimit returns the limit to use in a sql query, based on the fromNo and toNo set in the
	 * instance.
	 * 
	 * @return Integer limit
	 */
	Integer getLimit();

	/**
	 * getDelimiter composes as string to add to a sql query, to limit the result, based on the
	 * offset and limit calculated in the instance.
	 * 
	 * @return String delimiter
	 */
	String getDelimiter();

	/**
	 * getFromNo returns the fromNo set in the instance
	 * 
	 * @return fromNo
	 */
	Integer getFromNo();

	/**
	 * getToNo returns the toNo set in the instance
	 * 
	 * @return toNo
	 */
	Integer getToNo();

	/**
	 * delimiterIsPresent will return true if either offset and limit is set, false if both are null
	 * 
	 * @return boolean whether a delimiter is present or not
	 * 
	 */
	boolean delimiterIsPresent();

	/**
	 * setOrderBy sets the order by to use in a sql query
	 * 
	 * @param String
	 *            orderBy, the name of the column to order by
	 */
	void setOrderBy(String orderBy);

	/**
	 * setSortOrder sets the sort order to use in a sql query
	 * 
	 * @param {@link
	 *            SortOrder}, the sort order to set
	 */
	void setSortOrder(SortOrder sortOrder);

	/**
	 * getOrderByPartOfQuery uses the order by and the sort order set to compose a sort part of a
	 * sql query. If neither are set, an empty string SHOULD be returned
	 */
	String getOrderByPartOfQuery();

}
