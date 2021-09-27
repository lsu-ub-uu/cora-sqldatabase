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
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

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
	 * insertRowInTableWithValues creates a new row in database according to the specified
	 * TableQuery
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, parameters and values to add to the database
	 */
	void insertRowUsingQuery(TableQuery tableQuery);

	/**
	 * readRowsForQuery reads rows from a table or view as specified in the provided TableQuery
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to read rows
	 *            from the database.
	 * @return A List of Rows with the result of the query
	 */
	List<Row> readRowsForQuery(TableQuery tableQuery);

	/**
	 * readOneRowFromDbUsingTableAndConditions reads one row from the database as specified in the
	 * provided TableQuery.
	 * <p>
	 * Implementations MUST make sure that if no row or more than one row is found matching the
	 * conditions will a {@link SqlDatabaseException} be thrown, indicating that the requested
	 * single row can not be realibly read.
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to read one row
	 *            from the database.
	 * @return A Row with the result of the query
	 */
	Row readOneRowForQuery(TableQuery tableQuery);

	/**
	 * readNumberOfRows returns the numberOfRows in storage that matches the provided TableQuery.
	 * The returned number should be the same as the number of rows in the list returned by invoking
	 * {@link #readRowsForQuery(TableQuery)} with the same TableQuery.
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
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to update data
	 *            in the database.
	 */
	void updateRowsUsingQuery(TableQuery tableQuery);

	/**
	 * deleteRowsForQuery deletes rows from a table or view in the database according to the
	 * specified TableQuery
	 * 
	 * @param tableQuery
	 *            A TableQuery with the table, conditions and other settings to use to delete rows
	 *            from the database.
	 */
	void deleteRowsForQuery(TableQuery tableQuery);

	/**
	 * nextValueFromSequence returns the next value for the specified sequence
	 * <p>
	 * If the sequence does not exist MUST an {@link SQLException} thrown.
	 * 
	 * @param sequenceName
	 *            A String with the name of the sequenece to get the next value for
	 * @return a long with the next value of the sequence
	 */
	long nextValueFromSequence(String sequenceName);

	/**
	 * startTransaction starts a new transaction setting the underlying connection to
	 * autocommit(false). To commit the transaction run {@link #endTransaction()}.
	 */
	public void startTransaction();

	/**
	 * endTransaction ends the currently going transaction, and sets the underlying connection back
	 * to autocommit(true)
	 */
	public void endTransaction();

	/**
	 * rollback method calls a database rollback on an started transaction.
	 */
	void rollback();
}
