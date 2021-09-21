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
	// String tableName;
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
	 * setOrderBy sets the order by to use in a sql query
	 * 
	 * @param String
	 *            orderBy, the name of the column to order by
	 */
	// void setOrderBy(String orderBy);

	/**
	 * setSortOrder sets the sort order to use in a sql query
	 * 
	 * @param {@link
	 *            OrderCriteria}, the sort order to set
	 */
	// void setSortOrder(OrderCriteria sortOrder);

	/**
	 * factorUsingFromAndToNo factores a DbQueryInfo, using fromNo and toNo. The method SHOULD
	 * handle the possibilty that either fromNo or toNo is null, and return an instance of
	 * DbQueryInfo regardless.
	 * 
	 * @param Integer
	 *            fromNo, the from number to set in the DbQueryInfo
	 * 
	 * @param Integer
	 *            toNo, the to number to set in the DbQueryInfo
	 */
	// DbQueryInfo factorUsingFromNoAndToNo(Integer fromNo, Integer toNo);
	void setFromNo(Integer fromNo);

	void setToNo(Integer toNo);

	String assembleCreateSql();

	String assembleReadSql();

	String assembleUpdateSql();

	String assembleDeleteSql();

	List<Object> getQueryValues();

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
}
