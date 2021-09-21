/*
 * Copyright 2018, 2021 Uppsala University Library
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

import java.util.List;

import se.uu.ub.cora.sqldatabase.table.TableFacade;

/**
 * DatabaseFacade reads and changes data in a sql database using prepared statements.
 * <p>
 * If you only need to read and update data from one table at a time have a look at
 * {@link TableFacade} instead.
 * <p>
 * Implementations of DatabaseFacade are generally not threadsafe.
 */
public interface DatabaseFacade {
	/**
	 * readUsingSqlAndValues reads rows from the database using the supplied sql (prepared
	 * statement) and the supplied values.
	 * <p>
	 * If no is found matching the sql and values MUST an empty list be returned.
	 * 
	 * @param sql
	 *            A String with a prepared statement
	 * @param values
	 *            A List<Object> matching the values for the prepared statement
	 * @return A List<Row> with one entry in the list for each row with a map containing the
	 *         columnNames from the result as key and the corresponding values
	 */
	List<Row> readUsingSqlAndValues(String sql, List<Object> values);

	/**
	 * readOneRowOrFailUsingSqlAndValues reads one row from the database using the supplied sql
	 * (prepared statement) and the supplied values.
	 * <p>
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
	Row readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values);

	// TODO: update
	/**
	 * ExecuteSqlWithValues executes a sql statement as a preparedQuery returning the number of rows
	 * affected.
	 * 
	 * @param sql
	 *            A String with the prepared statement to execute
	 * @param values
	 *            A List with Objects to use in the prepared statement
	 * @return An int with the number of rows affected by the statement
	 */
	int executeSqlWithValues(String sql, List<Object> values);

}
