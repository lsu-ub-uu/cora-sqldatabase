/*
 * Copyright 2018, 2019 Uppsala University Library
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

import java.sql.Array;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.connection.SqlConnectionProvider;
import se.uu.ub.cora.logger.Logger;
import se.uu.ub.cora.logger.LoggerProvider;

public final class DataReaderImp implements DataReader {
	private static final String ERROR_READING_DATA_USING_SQL = "Error reading data using sql: ";
	private SqlConnectionProvider sqlConnectionProvider;
	private Logger log = LoggerProvider.getLoggerForClass(DataReaderImp.class);

	private DataReaderImp(SqlConnectionProvider sqlConnectionProvider) {
		this.sqlConnectionProvider = sqlConnectionProvider;
	}

	public static DataReaderImp usingSqlConnectionProvider(
			SqlConnectionProvider sqlConnectionProvider) {
		return new DataReaderImp(sqlConnectionProvider);
	}

	@Override
	public Map<String, Object> readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values) {
		List<Map<String, Object>> readRows = executePreparedStatementQueryUsingSqlAndValues(sql,
				values);
		throwErrorIfNoRowIsReturned(sql, readRows);
		throwErrorIfMoreThanOneRowIsReturned(sql, readRows);
		return getSingleResultFromList(readRows);
	}

	private void throwErrorIfNoRowIsReturned(String sql, List<Map<String, Object>> readRows) {
		if (readRows.isEmpty()) {
			throw SqlStorageException
					.withMessage(ERROR_READING_DATA_USING_SQL + sql + ": no row returned");
		}
	}

	private void throwErrorIfMoreThanOneRowIsReturned(String sql,
			List<Map<String, Object>> readRows) {
		if (resultHasMoreThanOneRow(readRows)) {
			throw SqlStorageException.withMessage(
					ERROR_READING_DATA_USING_SQL + sql + ": more than one row returned");
		}
	}

	private boolean resultHasMoreThanOneRow(List<Map<String, Object>> readRows) {
		return readRows.size() > 1;
	}

	private Map<String, Object> getSingleResultFromList(List<Map<String, Object>> readRows) {
		return readRows.get(0);
	}

	@Override
	public List<Map<String, Object>> executePreparedStatementQueryUsingSqlAndValues(String sql,
			List<Object> values) {
		try {
			return readUsingSqlAndValues(sql, values);
		} catch (SQLException e) {
			String message = ERROR_READING_DATA_USING_SQL + sql;
			log.logErrorUsingMessageAndException(message, e);
			throw SqlStorageException.withMessageAndException(message, e);
		}
	}

	private List<Map<String, Object>> readUsingSqlAndValues(String sql, List<Object> values)
			throws SQLException {

		try (Connection connection = sqlConnectionProvider.getConnection();
				PreparedStatement prepareStatement = connection.prepareStatement(sql);) {

			addParameterValuesToPreparedStatement(values, prepareStatement, connection);
			return getResultUsingQuery(prepareStatement);
		}
	}

	private void addParameterValuesToPreparedStatement(List<Object> values,
			PreparedStatement prepareStatement, Connection connection) throws SQLException {
		int position = 1;
		for (Object value : values) {
			if (value instanceof List) {
				Integer[] ids = { 2, 14, 4 };
				List<Integer> integerList = (List<Integer>) value;
				// Array array = connection.createArrayOf("integer", integerList.toArray());
				Array array = connection.createArrayOf("integer", ids);

				// prepareStatement.setObject(position, ids);
				prepareStatement.setArray(position, array);
			} else {
				prepareStatement.setObject(position, value);
			}
			position++;
		}
	}

	private List<Map<String, Object>> getResultUsingQuery(PreparedStatement prepareStatement)
			throws SQLException {
		try (ResultSet resultSet = prepareStatement.executeQuery();) {
			List<String> columnNames = createListOfColumnNamesFromResultSet(resultSet);
			return createListOfMapsFromResultSetUsingColumnNames(resultSet, columnNames);
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

	private List<Map<String, Object>> createListOfMapsFromResultSetUsingColumnNames(
			ResultSet resultSet, List<String> columnNames) throws SQLException {
		List<Map<String, Object>> all = new ArrayList<>();
		while (resultSet.next()) {
			HashMap<String, Object> row = createMapForCurrentRowInResultSet(resultSet, columnNames);
			all.add(row);
		}
		return all;
	}

	private HashMap<String, Object> createMapForCurrentRowInResultSet(ResultSet resultSet,
			List<String> columnNames) throws SQLException {
		HashMap<String, Object> row = new HashMap<>(columnNames.size());
		for (String columnName : columnNames) {
			row.put(columnName, resultSet.getObject(columnName));
		}
		return row;
	}

	public SqlConnectionProvider getSqlConnectionProvider() {
		// needed for test
		return sqlConnectionProvider;
	}
}