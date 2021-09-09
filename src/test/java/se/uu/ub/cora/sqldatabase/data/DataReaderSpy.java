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
package se.uu.ub.cora.sqldatabase.data;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

public class DataReaderSpy implements DataReader {

	public boolean executePreparedStatementQueryUsingSqlAndValuesWasCalled = false;
	public String sql = "";
	public List<Object> values;
	public List<Map<String, Object>> result = new ArrayList<>();
	public boolean throwError = false;
	public boolean readOneRowFromDbUsingTableAndConditionsWasCalled = false;
	public Map<String, Object> oneRowResult;

	@Override
	public List<Map<String, Object>> executePreparedStatementQueryUsingSqlAndValues(String sql,
			List<Object> values) {
		this.sql = sql;
		this.values = values;
		executePreparedStatementQueryUsingSqlAndValuesWasCalled = true;
		if (throwError) {
			throw SqlDatabaseException.withMessage(
					"Error from executePreparedStatementQueryUsingSqlAndValues in DataReaderSpy");
		}
		// Map<String, Object> innerResult = new HashMap<>();
		// if (sql.startsWith("select count")) {
		// innerResult.put("count", 453);
		// } else {
		Map<String, Object> innerResult = createResult();
		// }
		result.add(innerResult);
		return result;
	}

	private Map<String, Object> createResult() {
		Map<String, Object> innerResult = new HashMap<>();
		innerResult.put("id", "someId");
		innerResult.put("name", "someName");
		return innerResult;
	}

	@Override
	public Map<String, Object> readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values) {
		this.sql = sql;
		this.values = values;
		readOneRowFromDbUsingTableAndConditionsWasCalled = true;
		if (throwError) {
			throw SqlDatabaseException
					.withMessage("Error from readOneRowOrFailUsingSqlAndValues in DataReaderSpy");
		}

		oneRowResult = new HashMap<>();
		if (sql.startsWith("select count")) {
			oneRowResult.put("count", 453L);
		} else {
			oneRowResult = createResult();
		}
		return oneRowResult;
	}

}