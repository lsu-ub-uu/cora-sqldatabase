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

package se.uu.ub.cora.sqldatabase.table;

import java.sql.SQLException;
import java.util.List;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDataException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.SqlNotFoundException;

/**
 * TableFacade interacts with data from a sql database without the need to write sql statements.
 * <p>
 * If you need to use more generic sql statements use {@link DatabaseFacade} instead.
 * <p>
 * <em> TableFacade uses the AutoClosable structure to close used database resources. Clients using
 * an implementation of TableFacade MUST use a try-with-resources block or manually call the
 * {@link #close()} method to release the used database resources. </em>
 * <p>
 * Implementations of TableFacade are generally not threadsafe.
 */
public interface TableFacade extends AutoCloseable {

	/**
	 * insertRowUsingQuery creates a new row in database according to the specified TableQuery
	 * <p>
	 * If an execution finds a duplicate key a {@link SqlConflictException} MUST be thrown.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, parameters and values to add to the database
	 */
	void insertRowUsingQuery(TableQuery tableQuery);

	/**
	 * readRowsForQuery reads rows from a table or view as specified in the provided TableQuery
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to read rows
	 *            from the database.
	 * @return A List of Rows with the result of the query
	 */
	List<Row> readRowsForQuery(TableQuery tableQuery);

	/**
	 * readOneRowForQuery reads one row from the database as specified in the provided TableQuery.
	 * <p>
	 * Implementations MUST make sure that if no row is found matching the conditions will a
	 * {@link SqlNotFoundException} be thrown, indicating that the requested single row can not be
	 * realibly read.
	 * <p>
	 * Implementations MUST make sure that if no row or more than one row is found matching the
	 * conditions will a {@link SqlDataException} be thrown, indicating that the requested single
	 * row can not be realibly read.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table and conditions to use when reading one row from the
	 *            database.
	 * @return A Row with the result of the query
	 */
	Row readOneRowForQuery(TableQuery tableQuery);

	/**
	 * readNumberOfRows returns the numberOfRows in storage that matches the provided TableQuery.
	 * The returned number should be the same as the number of rows in the list returned by invoking
	 * {@link #readRowsForQuery(TableQuery)} with the same TableQuery.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to count the
	 *            number of rows that match this TableQuery.
	 * 
	 * @return A long with the number of rows matching the specified TableQuery
	 */
	long readNumberOfRows(TableQuery tableQuery);

	/**
	 * updateRowsUsingQuery updates rows in a table or view in the database according to the
	 * specified TableQuery
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to update data
	 *            in the database.
	 * @return An int with the number of rows updated by the tablequery
	 */
	int updateRowsUsingQuery(TableQuery tableQuery);

	/**
	 * deleteRowsForQuery deletes rows from a table or view in the database according to the
	 * specified TableQuery
	 * <p>
	 * If an execution finds a duplicate key a {@link SqlConflictException} MUST be thrown.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to delete rows
	 *            from the database.
	 * @return An int with the number of rows deleted by the tablequery
	 */
	int deleteRowsForQuery(TableQuery tableQuery);

	/**
	 * nextValueFromSequence returns the next value for the specified sequence
	 * <p>
	 * If the sequence does not exist MUST an {@link SQLException} thrown.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 * 
	 * @param sequenceName
	 *            A String with the name of the sequenece to get the next value for
	 * @return a long with the next value of the sequence
	 */
	long nextValueFromSequence(String sequenceName);

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
	 * rollback method calls a database rollback on an started transaction.
	 * <p>
	 * If an exception occurs while interacting with the database MUST an
	 * {@link SqlDatabaseException} be thrown.
	 */
	void rollback();

	/**
	 * close closes the underlying database resources.
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
