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
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

public interface TableQuery {
	/**
	 * The add method adds a parameter with name and value to this query.
	 * <p>
	 * If a parameter is to use null, MUST the value of the added parameter be {@link DatabaseNull}
	 * <p>
	 * If the name contains characters that are problematic for sql injection MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param name
	 *            A String with the name to use in the sql.
	 * @param value
	 *            A String with the value to use in the sql
	 *
	 */
	void addParameter(String name, Object value);

	/**
	 * The addCondition method adds a condition with name and value to this query.
	 * <p>
	 * If a condition is to use null, MUST the value of the added condition be {@link DatabaseNull}
	 * <p>
	 * If the name contains characters that are problematic for sql injection MUST an
	 * {@link SqlDatabaseException} be thrown.
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
	 * <p>
	 * If the name contains characters that are problematic for sql injection MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param column
	 *            A String with a column to order the result by
	 */
	void addOrderByAsc(String column);

	/**
	 * addOrderByDesc adds an descending order by column to the query, if more then one order by is
	 * added MUST they be added to generated sql in the order they are added.
	 * <p>
	 * If the name contains characters that are problematic for sql injection MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param column
	 *            A String with a column to order the result by
	 */
	void addOrderByDesc(String column);

	/**
	 * assembleCreateSql assembles an insert prepared statement sql based on the table and
	 * parameters added.
	 * 
	 * @return A String with a sql insert statement
	 */
	String assembleCreateSql();

	/**
	 * assembleReadSql assembles a read prepared statement sql based on the table, parameters,
	 * conditions, fromNo, toNo and sortorders added.
	 * 
	 * @return A String with an sql read statement
	 */
	String assembleReadSql();

	/**
	 * assembleUpdateSql assembles an update prepared statement sql based on the table, parameters
	 * and conditions added.
	 * 
	 * @return A String with a sql update statement
	 */
	String assembleUpdateSql();

	/**
	 * assembleDeleteSql assembles a delete prepared statement sql based on the table and conditions
	 * added.
	 * 
	 * @return A String with a sql delete statement
	 */
	String assembleDeleteSql();

	/**
	 * getQueryValues returns a list of values that is needed to set values in the prepared
	 * statements created by the assemble sql methods. The list of query values contains values from
	 * all parameters and conditions added.
	 * 
	 * @return A List of Objects containing values for the prepared statements
	 */
	List<Object> getQueryValues();

	/**
	 * assembleCountSql assembles an count prepared statement sql based on the table, parameters,
	 * conditions, fromNo, toNo added.
	 * 
	 * @return A String with a sql count statement
	 */
	String assembleCountSql();

}
