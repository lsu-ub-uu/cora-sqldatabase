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
package se.uu.ub.cora.sqldatabase.record.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import se.uu.ub.cora.sqldatabase.data.DataUpdater;
import se.uu.ub.cora.sqldatabase.record.RecordDeleter;

import java.util.StringJoiner;

public class RecordDeleterImp implements RecordDeleter {

	private DataUpdater dataUpdater;

	public RecordDeleterImp(DataUpdater dataUpdater) {
		this.dataUpdater = dataUpdater;
	}

	@Override
	public void deleteFromTableUsingConditions(String tableName, Map<String, Object> conditions) {
		StringBuilder sql = new StringBuilder("delete from " + tableName + " where ");
		List<String> columnNames = getAllColumnNames(conditions);
		appendColumnNamesToDeletePart(sql, columnNames);
		List<Object> columnValues = getAllColumnValues(conditions);

		dataUpdater.executeUsingSqlAndValues(sql.toString(), columnValues);

	}

	private List<String> getAllColumnNames(Map<String, Object> columnsWithValues) {
		List<String> columnNames = new ArrayList<>(columnsWithValues.size());
		for (Entry<String, Object> column : columnsWithValues.entrySet()) {
			columnNames.add(column.getKey());
		}
		return columnNames;
	}

	private String appendColumnNamesToDeletePart(StringBuilder sql, List<String> columnNames) {
		StringJoiner joiner = new StringJoiner(" and ");
		addAllToJoiner(columnNames, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private void addAllToJoiner(List<String> columnNames, StringJoiner joiner) {
		for (String columnName : columnNames) {
			joiner.add(columnName + " = ?");
		}
	}

	private List<Object> getAllColumnValues(Map<String, Object> columnsWithValues) {
		List<Object> columnValues = new ArrayList<>(columnsWithValues.size());
		for (Entry<String, Object> column : columnsWithValues.entrySet()) {
			columnValues.add(column.getValue());
		}
		return columnValues;
	}

	@Override
	public DataUpdater getDataUpdater() {
		return dataUpdater;
	}
}
