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
import se.uu.ub.cora.sqldatabase.record.RecordUpdater;

import java.util.StringJoiner;

public class RecordUpdaterImp implements RecordUpdater {

	private DataUpdater dataUpdater;

	public RecordUpdaterImp(DataUpdater dataUpdater) {
		this.dataUpdater = dataUpdater;
	}

	@Override
	public void updateTableUsingNameAndColumnsWithValuesAndConditions(String tableName,
			Map<String, Object> columnsWithValues, Map<String, Object> conditions) {

		StringBuilder sql = createSql(tableName, columnsWithValues, conditions);
		List<Object> valuesForUpdate = addColumnsAndConditionsToValuesForUpdate(columnsWithValues,
				conditions);

		dataUpdater.executeUsingSqlAndValues(sql.toString(), valuesForUpdate);
	}

	private StringBuilder createSql(String tableName, Map<String, Object> columnsWithValues,
			Map<String, Object> conditions) {
		StringBuilder sql = new StringBuilder(createSettingPartOfSqlStatement(tableName, columnsWithValues));
		sql.append(createWherePartOfSqlStatement(conditions));
		return sql;
	}

	private String createSettingPartOfSqlStatement(String tableName, Map<String, Object> columnsWithValues) {
		StringBuilder sql = new StringBuilder("update " + tableName + " set ");
		List<String> columnNames = getAllColumnNames(columnsWithValues);
		return appendColumnsToSelectPart(sql, columnNames);
	}

	private List<String> getAllColumnNames(Map<String, Object> columnsWithValues) {
		List<String> columnNames = new ArrayList<>(columnsWithValues.size());
		for (Entry<String, Object> column : columnsWithValues.entrySet()) {
			columnNames.add(column.getKey());
		}
		return columnNames;
	}

	private String appendColumnsToSelectPart(StringBuilder sql, List<String> columnNames) {
		StringJoiner joiner = new StringJoiner(", ");
		addAllToJoiner(columnNames, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private void addAllToJoiner(List<String> columnNames, StringJoiner joiner) {
		for (String columnName : columnNames) {
			joiner.add(columnName + " = ?");
		}
	}

	private String createWherePartOfSqlStatement(Map<String, Object> conditions) {
		StringBuilder sql = new StringBuilder(" where ");
		List<String> conditionNames = getAllConditionNames(conditions);
		return appendConditionsToWherePart(sql, conditionNames);
	}

	private List<String> getAllConditionNames(Map<String, Object> conditions) {
		List<String> conditionNames = new ArrayList<>(conditions.size());
		for (Entry<String, Object> condition : conditions.entrySet()) {
			conditionNames.add(condition.getKey());
		}
		return conditionNames;
	}

	public DataUpdater getDataUpdater() {
		return dataUpdater;
	}

	private String appendConditionsToWherePart(StringBuilder sql, List<String> conditions) {
		StringJoiner joiner = new StringJoiner(" and ");
		addAllToJoiner(conditions, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private List<Object> addColumnsAndConditionsToValuesForUpdate(Map<String, Object> columns,
			Map<String, Object> conditions) {
		List<Object> valuesForUpdate = new ArrayList<>();
		valuesForUpdate.addAll(columns.values());
		valuesForUpdate.addAll(conditions.values());
		return valuesForUpdate;
	}

}
