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

package se.uu.ub.cora.sqldatabase.table.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringJoiner;

import se.uu.ub.cora.sqldatabase.Conditions;
import se.uu.ub.cora.sqldatabase.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.data.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public final class TableFacadeImp implements TableFacade {
	private static final int MIN_FROM_NUMBER = 1;
	private static final String ERROR_READING_DATA_FROM = "Error reading data from ";
	private DatabaseFacade dbFacade;
	private static final String NEXTVAL_COLUMN_NAME = "nextval";

	private TableFacadeImp(DatabaseFacade databaseFacade) {
		this.dbFacade = databaseFacade;
	}

	public static TableFacadeImp usingDataReader(DatabaseFacade dataReader) {
		return new TableFacadeImp(dataReader);
	}

	@Override
	public List<Row> readRowsFromTable(String tableName) {
		String sql = createSelectAllFor(tableName);
		return readAllFromTableUsingSql(tableName, sql);
	}

	private List<Row> readAllFromTableUsingSql(String tableName, String sql) {
		try {
			return tryToReadAllFromTable(sql);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private List<Row> tryToReadAllFromTable(String sql) {
		return dbFacade.readUsingSqlAndValues(sql, Collections.emptyList());

	}

	private String createSelectAllFor(String tableName) {
		return "select * from " + tableName;
	}

	@Override
	public Row readOneRowFromTableUsingConditions(String tableName, Conditions conditions) {
		try {
			return tryToReadOneRowFromDbUsingTableAndConditions(tableName, conditions);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private Row tryToReadOneRowFromDbUsingTableAndConditions(String tableName,
			Conditions conditions) {
		String sql = createSqlForTableNameAndConditionsNames(tableName, conditions.getNames());
		return dbFacade.readOneRowOrFailUsingSqlAndValues(sql, conditions.getValues());
	}

	private String createSqlForTableNameAndConditionsNames(String tableName,
			List<String> conditionName) {
		return createSelectAllFor(tableName) + possiblyAddConditionsToSql(conditionName);
	}

	private String createConditionPartOfSql(List<String> conditionName) {
		StringJoiner joiner = new StringJoiner(" and ");
		for (String key : conditionName) {
			joiner.add(key + " = ?");
		}
		return joiner.toString();
	}

	@Override
	public List<Row> readRowsFromTableUsingConditions(String tableName, Conditions conditions) {
		try {
			return tryToReadFromTableUsingConditions(tableName, conditions);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private List<Row> tryToReadFromTableUsingConditions(String tableName, Conditions conditions) {
		String sql = createSqlForTableNameAndConditionsNames(tableName, conditions.getNames());
		return dbFacade.readUsingSqlAndValues(sql, conditions.getValues());
	}

	public DatabaseFacade getDataReader() {
		// needed for test
		return dbFacade;
	}

	@Override
	public long nextValueFromSequence(String sequenceName) {
		String statement = "select nextval('" + sequenceName + "') as " + NEXTVAL_COLUMN_NAME;
		Row row = dbFacade.readOneRowOrFailUsingSqlAndValues(statement, Collections.emptyList());
		return (long) row.getValueByColumn(NEXTVAL_COLUMN_NAME);
	}

	@Override
	public List<Row> readRowsFromTable(String tableName, DbQueryInfo queryInfo) {
		String sql = createSelectAllFor(tableName);
		sql += getSortPart(queryInfo);
		sql += getDelimiter(queryInfo);
		return readAllFromTableUsingSql(tableName, sql);
	}

	private String getSortPart(DbQueryInfo queryInfo) {
		return queryInfo.getOrderByPartOfQuery();
	}

	private String getDelimiter(DbQueryInfo queryInfo) {
		return queryInfo.getDelimiter();
	}

	@Override
	public long numberOfRowsInTableForConditionsAndQueryInfo(String tableName,
			Conditions conditions, DbQueryInfo queryInfo) {
		long numberOfRows = readNumberOfRows(tableName, conditions);

		if (queryInfo.delimiterIsPresent()) {
			return calculateNumberOfRowsUsingDelimiter(queryInfo, numberOfRows);
		}
		return numberOfRows;
	}

	private long readNumberOfRows(String tableName, Conditions conditions) {
		String sql = assembleSqlForNumberOfRows(tableName, conditions);
		Row countResult = dbFacade.readOneRowOrFailUsingSqlAndValues(sql, conditions.getValues());
		return (long) countResult.getValueByColumn("count");

	}

	private long calculateNumberOfRowsUsingDelimiter(DbQueryInfo queryInfo, long numberOfRows) {
		long maxToNumber = toNoIsNullOrTooLarge(queryInfo.getToNo(), numberOfRows) ? numberOfRows
				: queryInfo.getToNo();
		long minFromNumber = fromNoIsNullOrLessThanMinNumber(queryInfo) ? MIN_FROM_NUMBER
				: queryInfo.getFromNo();

		return fromLargerThanTo(minFromNumber, maxToNumber) ? 0
				: calculateDifference(minFromNumber, maxToNumber);
	}

	private String assembleSqlForNumberOfRows(String tableName, Conditions conditions) {
		return "select count(*) from " + tableName
				+ possiblyAddConditionsToSql(conditions.getNames());
	}

	private String possiblyAddConditionsToSql(List<String> conditionsName) {
		if (!conditionsName.isEmpty()) {
			String conditionPart = createConditionPartOfSql(conditionsName);
			return " where " + conditionPart;
		}
		return "";
	}

	private boolean fromNoIsNullOrLessThanMinNumber(DbQueryInfo queryInfo) {
		return queryInfo.getFromNo() == null || queryInfo.getFromNo() < MIN_FROM_NUMBER;
	}

	private boolean toNoIsNullOrTooLarge(Integer toNo, long numberOfRows) {
		return toNo == null || toNo > numberOfRows;
	}

	private boolean fromLargerThanTo(long minFromNumber, long maxToNumber) {
		return minFromNumber > maxToNumber;
	}

	private long calculateDifference(long minFromNumber, long maxToNumber) {
		return maxToNumber - minFromNumber + 1;
	}

	@Override
	public void insertRowInTableWithValues(String tableName, Map<String, Object> values) {
		StringBuilder sql = createSql(tableName, values);
		List<Object> columnValues = getAllColumnValues(values);
		dbFacade.executeSqlWithValues(sql.toString(), columnValues);

	}

	private StringBuilder createSql(String tableName, Map<String, Object> columnsWithValues) {
		StringBuilder sql = new StringBuilder("insert into " + tableName + "(");
		List<String> columnNames = getAllColumnNames(columnsWithValues);
		appendColumnNamesToInsertPart(sql, columnNames);
		appendValuesPart(sql, columnNames);
		return sql;
	}

	private String appendColumnNamesToInsertPart(StringBuilder sql, List<String> columnNames) {
		StringJoiner joiner = new StringJoiner(", ");
		addAllToJoiner2(columnNames, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private void addAllToJoiner2(List<String> columnNames, StringJoiner joiner) {
		for (String columnName : columnNames) {
			joiner.add(columnName);
		}
	}

	private void appendValuesPart(StringBuilder sql, List<String> columnNames) {
		sql.append(") values(");
		sql.append(addCorrectNumberOfPlaceHoldersForValues(columnNames));
		sql.append(')');
	}

	private String addCorrectNumberOfPlaceHoldersForValues(List<String> columnNames) {
		StringJoiner joiner = new StringJoiner(", ");
		for (int i = 0; i < columnNames.size(); i++) {
			joiner.add("?");
		}
		return joiner.toString();
	}

	@Override
	public List<Row> readRowsFromTableUsingConditionsAndQueryInfo(String tableName,
			Conditions conditions, DbQueryInfo queryInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRowInTableUsingValuesAndConditions(String tableName,
			Map<String, Object> values, Conditions conditions) {
		StringBuilder sql = createSql(tableName, values, conditions);
		List<Object> valuesForUpdate = getListOfValuesFromValuesAndConditions(values, conditions);
		dbFacade.executeSqlWithValues(sql.toString(), valuesForUpdate);
	}

	private StringBuilder createSql(String tableName, Map<String, Object> columnsWithValues,
			Conditions conditions) {
		StringBuilder sql = new StringBuilder(
				createSettingPartOfSqlStatement(tableName, columnsWithValues));
		sql.append(createWherePartOfSqlStatement(conditions));
		return sql;
	}

	private String createSettingPartOfSqlStatement(String tableName,
			Map<String, Object> columnsWithValues) {
		StringBuilder sql = new StringBuilder("update " + tableName + " set ");
		List<String> columnNames = getAllColumnNames(columnsWithValues);
		return appendColumnsToSelectPart(sql, columnNames);
	}

	private String appendColumnsToSelectPart(StringBuilder sql, List<String> columnNames) {
		StringJoiner joiner = new StringJoiner(", ");
		addAllToJoiner(columnNames, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private String createWherePartOfSqlStatement(Conditions conditions) {
		StringBuilder sql = new StringBuilder(" where ");
		return appendConditionsToWherePart(sql, conditions.getNames());
	}

	private String appendConditionsToWherePart(StringBuilder sql, List<String> conditions) {
		StringJoiner joiner = new StringJoiner(" and ");
		addAllToJoiner(conditions, joiner);
		sql.append(joiner);
		return sql.toString();
	}

	private List<Object> getListOfValuesFromValuesAndConditions(Map<String, Object> values,
			Conditions conditions) {
		List<Object> valuesForUpdate = new ArrayList<>();
		valuesForUpdate.addAll(values.values());
		valuesForUpdate.addAll(conditions.getValues());
		return valuesForUpdate;
	}

	@Override
	public void deleteRowFromTableUsingConditions(String tableName, Conditions conditions) {
		StringBuilder sql = new StringBuilder("delete from " + tableName + " where ");
		appendColumnNamesToDeletePart(sql, conditions.getNames());
		dbFacade.executeSqlWithValues(sql.toString(), conditions.getValues());
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
}