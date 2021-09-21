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

import java.util.List;

import se.uu.ub.cora.sqldatabase.DatabaseNull;

public interface TableQuery {
	/**
	 * The add method adds a parameter with name and value to this parameters.
	 * <p>
	 * If a parameter is to use null, MUST the value of the added parameter be {@link DatabaseNull}
	 * 
	 * @param name
	 *            A String with the name to use in the sql query
	 * 
	 * @param value
	 *            A String with the value to use in the sql query
	 *
	 */
	void addParameter(String name, Object value);

	/**
	 * The add method adds a condition with name and value to this conditions.
	 * <p>
	 * If a condition is to use null, MUST the value of the added condition be {@link DatabaseNull}
	 * 
	 * @param name
	 *            A String with the name to use in the sql query
	 * 
	 * @param value
	 *            A String with the value to use in the sql query
	 *
	 */
	void addCondition(String name, Object value);

	/**
	 * getOffset returns the offset to use in a sql query, based on the fromNo set in the instance.
	 * FromNo is expected to be 1 if the result should start with the first record. Offset however
	 * starts at zero, and will never be lower than zero. If no fromNo is set in the instance, zero
	 * will be returned.
	 * 
	 * @return Integer offset
	 */

	/**
	 * setFromNo sets the from number (in the result), indicating the first record that the
	 * generated sql query should return. A from number of 1 is for the first record in the
	 * resultset.
	 * 
	 * @param fromNo
	 *            An Integer with the from number
	 */
	void setFromNo(Long fromNo);

	/**
	 * setToNo sets the to number (in the result), indicating the last record that the generated sql
	 * query should return. A to number of 10 should return the first 10 records in the resultset.
	 * 
	 * @param toNo
	 *            An Integer with the to number
	 */
	void setToNo(Long toNo);

	/**
	 * addOrderByAsc adds an ascending order by column to the query, if more then one order by is
	 * added MUST they be added to generated sql in the order they are added.
	 * 
	 * @param column
	 *            A String with a column to order the result by
	 */
	void addOrderByAsc(String column);

	/**
	 * addOrderByDesc adds an descending order by column to the query, if more then one order by is
	 * added MUST they be added to generated sql in the order they are added.
	 * 
	 * @param column
	 *            A String with a column to order the result by
	 */
	void addOrderByDesc(String column);

	String assembleCreateSql();

	String assembleReadSql();

	String assembleUpdateSql();

	String assembleDeleteSql();

	List<Object> getQueryValues();

	String assembleCountSql();

}
