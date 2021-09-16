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
import java.util.StringJoiner;

import se.uu.ub.cora.sqldatabase.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.data.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.data.Row;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public final class TableFacadeImp implements TableFacade {
	private static final int MIN_FROM_NUMBER = 1;
	private static final String ERROR_READING_DATA_FROM = "Error reading data from ";
	private DatabaseFacade dataReader;
	private static final String NEXTVAL_COLUMN_NAME = "nextval";

	private TableFacadeImp(DatabaseFacade dataReader) {
		this.dataReader = dataReader;
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
		return dataReader.readUsingSqlAndValues(sql,
				Collections.emptyList());

	}

	private String createSelectAllFor(String tableName) {
		return "select * from " + tableName;
	}

	@Override
	public Row readOneRowFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		try {
			return tryToReadOneRowFromDbUsingTableAndConditions(tableName, conditions);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private Row tryToReadOneRowFromDbUsingTableAndConditions(String tableName,
			Map<String, Object> conditions) {

		List<Object> values = new ArrayList<>();
		values.addAll(conditions.values());

		String sql = createSqlForTableNameAndConditions(tableName, conditions);
		return dataReader.readOneRowOrFailUsingSqlAndValues(sql, values);
	}

	private String createSqlForTableNameAndConditions(String tableName,
			Map<String, Object> conditions) {
		return "select * from " + tableName + possiblyAddConditionsToSql(conditions);
	}

	private String createConditionPartOfSql(Map<String, Object> conditions) {
		StringJoiner joiner = new StringJoiner(" and ");
		for (String key : conditions.keySet()) {
			joiner.add(key + " = ?");
		}
		return joiner.toString();
	}

	@Override
	public List<Row> readRowsFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		try {
			return tryToReadFromTableUsingConditions(tableName, conditions);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private List<Row> tryToReadFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		String sql = createSqlForTableNameAndConditions(tableName, conditions);
		List<Object> values = new ArrayList<>();
		values.addAll(conditions.values());
		return dataReader.readUsingSqlAndValues(sql, values);
	}

	public DatabaseFacade getDataReader() {
		// needed for test
		return dataReader;
	}

	@Override
	public long nextValueFromSequence(String sequenceName) {
		String statement = "select nextval('" + sequenceName + "') as " + NEXTVAL_COLUMN_NAME;
		Row row = dataReader.readOneRowOrFailUsingSqlAndValues(statement, Collections.emptyList());
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
			Map<String, Object> conditions, DbQueryInfo queryInfo) {
		long numberOfRows = readNumberOfRows(tableName, conditions);

		if (queryInfo.delimiterIsPresent()) {
			return calculateNumberOfRowsUsingDelimiter(queryInfo, numberOfRows);
		}
		return numberOfRows;
	}

	private long readNumberOfRows(String tableName, Map<String, Object> conditions) {
		String sql = assembleSqlForNumberOfRows(tableName, conditions);
		List<Object> values = getConditionsAsValues(conditions);
		Row countResult = dataReader.readOneRowOrFailUsingSqlAndValues(sql, values);
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

	private String assembleSqlForNumberOfRows(String tableName, Map<String, Object> conditions) {
		return "select count(*) from " + tableName + possiblyAddConditionsToSql(conditions);
	}

	private List<Object> getConditionsAsValues(Map<String, Object> conditions) {
		List<Object> values = new ArrayList<>();
		values.addAll(conditions.values());
		return values;
	}

	private String possiblyAddConditionsToSql(Map<String, Object> conditions) {
		if (!conditions.isEmpty()) {
			String conditionPart = createConditionPartOfSql(conditions);
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
		// TODO Auto-generated method stub

	}

	@Override
	public List<Row> readRowsFromTableUsingConditionsAndQueryInfo(String tableName,
			Map<String, Object> conditions, DbQueryInfo queryInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRowInTableUsingValuesAndConditions(String tableName,
			Map<String, Object> values, Map<String, Object> conditions) {
		// TODO Auto-generated method stub

	}

	@Override
	public void deleteRowFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		// TODO Auto-generated method stub

	}

}