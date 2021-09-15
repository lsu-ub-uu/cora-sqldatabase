/*
 * Copyright 2018 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.data;

import java.util.List;
import java.util.Map;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

/**
 * DataReader reads data from a sql database using prepared statements.
 * <p>
 * If you only need to read data from one table have a look at {@link TableFacade} instead.
 */
public interface DataReader {
	/**
	 * executePreparedStatementQueryUsingSqlAndValues reads rows from the database using the
	 * supplied sql (prepared statement) and the supplied values.<br>
	 * If no is found matching the sql and values MUST an empty list be returned.
	 * 
	 * @param sql
	 *            A String with a prepared statement
	 * @param values
	 *            A List<Object> matching the values for the prepared statement
	 * @return A List<Row> with one entry in the list for each row with a map containing the
	 *         columnNames from the result as key and the corresponding values
	 */
	List<Row> executePreparedStatementQueryUsingSqlAndValues(String sql, List<Object> values);

	/**
	 * readOneRowOrFailUsingSqlAndValues reads one row from the database using the supplied sql
	 * (prepared statement) and the supplied values.<br>
	 * If no row or more than one row is found matching the sql and values MUST a
	 * {@link SqlDatabaseException} be thrown, indicating that the requested single row can not be
	 * realibly read.
	 * 
	 * @param sql
	 *            A String with a prepared statement
	 * @param values
	 *            A List<Object> matching the values for the prepared statement
	 * @return A Map<String, Object> with the columnNames from the result as key and the
	 *         corresponding values
	 */
	Map<String, Object> readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values);

}
