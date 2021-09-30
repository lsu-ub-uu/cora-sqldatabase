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

import java.sql.Connection;
import java.util.List;

import se.uu.ub.cora.sqldatabase.table.TableFacade;

/**
 * DatabaseFacade reads and changes data in a sql database using prepared statements.
 * <p>
 * If you only need to read and update data from one table at a time have a look at
 * {@link TableFacade} instead.
 * <p>
 * <em> DatabaseFacade uses the AutoClosable structure to close used database resources. Clients
 * using an implementation of DatabaseFacade MUST use a try-with-resources block or manually call
 * the {@link #close()} method to release the used database resources. </em>
 * <p>
 * Implementations of DatabaseFacade MUST ensure that the underlying {@link Connection} is using
 * autocommit(true) (transaction support is turned off) if {@link #startTransaction()} has not been
 * called. Using transactions is an active choice. If a client wants to use a transaction must the
 * {@link #startTransaction()} method be called and to finnish the transaction must the
 * {@link #endTransaction()} be called.
 * <p>
 * Implementations of DatabaseFacade are generally not threadsafe.
 */
public interface DatabaseFacade extends AutoCloseable {
	/**
	 * readUsingSqlAndValues reads rows from the database using the supplied sql (prepared
	 * statement) and the supplied values.
	 * <p>
	 * If no result is found matching the sql and values MUST an empty list be returned.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param sql
	 *            A String with a prepared statement
	 * @param values
	 *            A List of Objects matching the values for the prepared statement
	 * @return A List with {@link Row}s with one entry in the list for each row in the result
	 */
	List<Row> readUsingSqlAndValues(String sql, List<Object> values);

	/**
	 * readOneRowOrFailUsingSqlAndValues reads one row from the database using the supplied sql
	 * (prepared statement) and the supplied values.
	 * <p>
	 * If no row or more than one row is found matching the sql and values MUST a
	 * {@link SqlDatabaseException} be thrown, indicating that the requested single row can not be
	 * realibly read.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param sql
	 *            A String with a prepared statement
	 * @param values
	 *            A List<Object> matching the values for the prepared statement
	 * @return A Row with the read row
	 */
	Row readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values);

	/**
	 * executeSqlWithValues executes a sql statement as a preparedQuery returning the number of rows
	 * affected.
	 * <p>
	 * If an executions finds a duplicate key a {@link SqlConflictException} MUST be thrown,
	 * indicating that the execution will not be performed.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param sql
	 *            A String with the prepared statement to execute
	 * @param values
	 *            A List with Objects to use in the prepared statement
	 * @return An int with the number of rows affected by the statement
	 */
	int executeSqlWithValues(String sql, List<Object> values);

	/**
	 * startTransaction starts a new transaction setting the underlying connection to
	 * autocommit(false). To commit the transaction run {@link #endTransaction()}.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 */
	public void startTransaction();

	/**
	 * endTransaction ends the currently going transaction, and sets the underlying connection back
	 * to autocommit(true)
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 */
	public void endTransaction();

	/**
	 * rollback rollbacks a started transaction.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 */
	void rollback();

	/**
	 * close closes the underlying connection.
	 * <p>
	 * Implementations MUST make sure that if a transaction is started but not ended, is rollback
	 * called and a {@link SqlDatabaseException} is thrown.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 */
	@Override
	void close();
}
