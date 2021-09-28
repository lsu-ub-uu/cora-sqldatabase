/*
 * Copyright 2018, 2019, 2021 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.internal;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.DatabaseNull;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;

public final class DatabaseFacadeImp implements DatabaseFacade {
	private SqlConnectionProvider sqlConnectionProvider;
	private Connection connection;
	private Logger log = LoggerProvider.getLoggerForClass(DatabaseFacadeImp.class);
	private static final int SQL_NULL = java.sql.Types.NULL;
	private static final String ERROR_READING_DATA_USING_SQL = "Error reading data using sql: ";

	private DatabaseFacadeImp(SqlConnectionProvider sqlConnectionProvider) {
		this.sqlConnectionProvider = sqlConnectionProvider;
	}

	public static DatabaseFacadeImp usingSqlConnectionProvider(
			SqlConnectionProvider sqlConnectionProvider) {
		return new DatabaseFacadeImp(sqlConnectionProvider);
	}

	@Override
	public Row readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values) {
		List<Row> readRows = readUsingSqlAndValues(sql, values);
		return tryToReadOnRow(sql, readRows);
	}

	private Row tryToReadOnRow(String sql, List<Row> readRows) {
		throwErrorIfLessOrMoreThanOneRowRead(sql, readRows);
		return getFirstRowFromReadRows(readRows);
	}

	private void throwErrorIfLessOrMoreThanOneRowRead(String sql, List<Row> readRows) {
		throwErrorIfNoRowIsReturned(sql, readRows);
		throwErrorIfMoreThanOneRowIsReturned(sql, readRows);
	}

	private void throwErrorIfNoRowIsReturned(String sql, List<Row> readRows) {
		if (readRows.isEmpty()) {
			throw SqlDatabaseException
					.withMessage(ERROR_READING_DATA_USING_SQL + sql + ": no row returned");
		}
	}

	private void throwErrorIfMoreThanOneRowIsReturned(String sql, List<Row> readRows) {
		if (resultHasMoreThanOneRow(readRows)) {
			throw SqlDatabaseException.withMessage(
					ERROR_READING_DATA_USING_SQL + sql + ": more than one row returned");
		}
	}

	private boolean resultHasMoreThanOneRow(List<Row> readRows) {
		return readRows.size() > 1;
	}

	private Row getFirstRowFromReadRows(List<Row> readRows) {
		return readRows.get(0);
	}

	@Override
	public List<Row> readUsingSqlAndValues(String sql, List<Object> values) {
		try {
			return tryToReadUsingSqlAndValues(sql, values);
		} catch (SQLException e) {
			String message = ERROR_READING_DATA_USING_SQL + sql;
			log.logErrorUsingMessageAndException(message, e);
			throw throwSqlDatabaseException(message, e);
		}
	}

	private SqlDatabaseException throwSqlDatabaseException(String message, SQLException e) {
		return SqlDatabaseException.withMessageAndException(message, e);
	}

	private List<Row> tryToReadUsingSqlAndValues(String sql, List<Object> values)
			throws SQLException {
		createConnectionIfNotCreatedSinceBefore();
		try (PreparedStatement prepareStatement = connection.prepareStatement(sql);) {
			addValuesToPreparedStatement(values, prepareStatement);
			return getResultUsingQuery(prepareStatement);
		}
	}

	private void addValuesToPreparedStatement(List<Object> values,
			PreparedStatement preparedStatement) throws SQLException {
		int position = 1;
		for (Object value : values) {
			setValueUsingPoperType(preparedStatement, position, value);
			position++;
		}
	}

	private void setValueUsingPoperType(PreparedStatement preparedStatement, int position,
			Object value) throws SQLException {
		if (isTimestamp(value)) {
			preparedStatement.setTimestamp(position, (Timestamp) value);
		} else if (isDatabaseNull(value)) {
			preparedStatement.setNull(position, SQL_NULL);
		} else {
			preparedStatement.setObject(position, value);
		}
	}

	private boolean isTimestamp(Object value) {
		return value instanceof Timestamp;
	}

	private boolean isDatabaseNull(Object value) {
		return value instanceof DatabaseNull;
	}

	private List<Row> getResultUsingQuery(PreparedStatement prepareStatement) throws SQLException {
		try (ResultSet result = prepareStatement.executeQuery();) {
			List<String> columnNames = createListOfColumnNamesFromResultSet(result);
			return createListOfMapsFromResultSetUsingColumnNames(result, columnNames);
		}
	}

	private List<String> createListOfColumnNamesFromResultSet(ResultSet resultSet)
			throws SQLException {
		ResultSetMetaData metaData = resultSet.getMetaData();
		int columnCount = metaData.getColumnCount();
		return createListOfColumnNamesFromMetadata(metaData, columnCount);
	}

	private List<String> createListOfColumnNamesFromMetadata(ResultSetMetaData metaData,
			int columnCount) throws SQLException {
		List<String> columnNames = new ArrayList<>(columnCount);
		for (int i = 1; i <= columnCount; i++) {
			columnNames.add(metaData.getColumnName(i));
		}
		return columnNames;
	}

	private List<Row> createListOfMapsFromResultSetUsingColumnNames(ResultSet resultSet,
			List<String> columnNames) throws SQLException {
		List<Row> all = new ArrayList<>();
		while (resultSet.next()) {
			Row row = createMapForCurrentRowInResultSet(resultSet, columnNames);
			all.add(row);
		}
		return all;
	}

	private Row createMapForCurrentRowInResultSet(ResultSet resultSet, List<String> columnNames)
			throws SQLException {
		RowImp row = new RowImp();
		for (String columnName : columnNames) {
			row.addColumnWithValue(columnName, resultSet.getObject(columnName));
		}
		return row;
	}

	@Override
	public int executeSqlWithValues(String sql, List<Object> values) {
		try {
			return updateUsingSqlAndValues(sql, values);
		} catch (SQLException e) {
			throw throwSqlDatabaseException("Error executing statement: " + sql, e);
		}
	}

	private int updateUsingSqlAndValues(String sql, List<Object> values) throws SQLException {
		createConnectionIfNotCreatedSinceBefore();
		try (PreparedStatement prepareStatement = connection.prepareStatement(sql);) {
			addValuesToPreparedStatement(values, prepareStatement);
			return prepareStatement.executeUpdate();
		}
	}

	private void createConnectionIfNotCreatedSinceBefore() {
		if (noConnection()) {
			connection = sqlConnectionProvider.getConnection();
		}
	}

	private boolean noConnection() {
		return !connectionExists();
	}

	private boolean connectionExists() {
		return connection != null;
	}

	@Override
	public void close() {
		closingConnectionUsingAutoClosable();
	}

	private void closingConnectionUsingAutoClosable() {
		try (Connection makingConnectionAutoClosable = connection) {
			rollbackAndThrowExceptionIfTransactionIsNotEnded();
		} catch (SQLException e) {
			throw throwSqlDatabaseException("Error closing connection.", e);
		}
	}

	private void rollbackAndThrowExceptionIfTransactionIsNotEnded() throws SQLException {
		if (transactionIsStarted()) {
			rollback();
			throw SqlDatabaseException
					.withMessage("Close called on running transaction, rollback perfromed.");
		}
	}

	private boolean transactionIsStarted() throws SQLException {
		return connectionExists() && !connection.getAutoCommit();
	}

	@Override
	public void startTransaction() {
		try {
			createConnectionIfNotCreatedSinceBefore();
			connection.setAutoCommit(false);
		} catch (SQLException e) {
			throw throwSqlDatabaseException("Error starting transaction.", e);
		}
	}

	@Override
	public void endTransaction() {
		try {
			connection.setAutoCommit(true);
		} catch (SQLException e) {
			throw throwSqlDatabaseException("Error ending transaction.", e);
		}

	}

	public SqlConnectionProvider getSqlConnectionProvider() {
		// needed for test
		return sqlConnectionProvider;
	}

	@Override
	public void rollback() {
		try {
			connection.rollback();
		} catch (SQLException e) {
			throw throwSqlDatabaseException("Error doing rollBack on connection.", e);
		}

	}

}