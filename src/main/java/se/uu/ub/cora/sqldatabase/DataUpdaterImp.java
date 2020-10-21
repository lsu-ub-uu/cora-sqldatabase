/*
 * Copyright 2019 Uppsala University Library
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
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import org.postgresql.util.PGobject;

import se.uu.ub.cora.connection.SqlConnectionProvider;
import se.uu.ub.cora.json.parser.JsonObject;

public class DataUpdaterImp implements DataUpdater {

	private SqlConnectionProvider sqlConnectionProvider;

	public static DataUpdaterImp usingSqlConnectionProvider(
			SqlConnectionProvider sqlConnectionProvider) {
		return new DataUpdaterImp(sqlConnectionProvider);
	}

	private DataUpdaterImp(SqlConnectionProvider sqlConnectionProvider) {
		this.sqlConnectionProvider = sqlConnectionProvider;
	}

	@Override
	public int executeUsingSqlAndValues(String sql, List<Object> values) {
		try {
			return updateUsingSqlAndValues(sql, values);
		} catch (SQLException e) {
			throw SqlStorageException.withMessageAndException("Error executing statement: " + sql,
					e);
		}
	}

	private int updateUsingSqlAndValues(String sql, List<Object> values) throws SQLException {
		try (Connection connection = sqlConnectionProvider.getConnection();
				PreparedStatement prepareStatement = connection.prepareStatement(sql);) {
			addParameterValuesToPreparedStatement(values, prepareStatement);
			return prepareStatement.executeUpdate();
		}
	}

	private void addParameterValuesToPreparedStatement(List<Object> values,
			PreparedStatement preparedStatement) throws SQLException {
		int position = 1;
		for (Object value : values) {
			if (value instanceof Timestamp) {
				preparedStatement.setTimestamp(position, (Timestamp) value);
			} else if (value instanceof JsonObject) {
				PGobject pgObject = createPgObjectFromJsonValue(value);
				preparedStatement.setObject(position, pgObject);
			} else {
				preparedStatement.setObject(position, value);
			}
			position++;
		}
	}

	private PGobject createPgObjectFromJsonValue(Object value) throws SQLException {
		String jsonFormattedString = getJsonValueAsString(value);
		return createPgObject(jsonFormattedString);
	}

	private String getJsonValueAsString(Object value) {
		JsonObject jsonObject = (JsonObject) value;
		return jsonObject.toJsonFormattedString();
	}

	private PGobject createPgObject(String jsonFormattedString) throws SQLException {
		PGobject pgObject = new PGobject();
		pgObject.setType("json");
		pgObject.setValue(jsonFormattedString);
		return pgObject;
	}

	public SqlConnectionProvider getSqlConnectionProvider() {
		// needed for test
		return sqlConnectionProvider;
	}
}
