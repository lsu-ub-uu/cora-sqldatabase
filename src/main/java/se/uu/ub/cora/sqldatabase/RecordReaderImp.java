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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;

public final class RecordReaderImp implements RecordReader {
	private static final int MIN_FROM_NUMBER = 1;
	private static final String ERROR_READING_DATA_FROM = "Error reading data from ";
	private DataReader dataReader;

	private RecordReaderImp(DataReader dataReader) {
		this.dataReader = dataReader;
	}

	public static RecordReaderImp usingDataReader(DataReader dataReader) {
		return new RecordReaderImp(dataReader);
	}

	@Override
	public List<Map<String, Object>> readAllFromTable(String tableName) {
		String sql = createSelectAllFor(tableName);
		return readAllFromTableUsingSql(tableName, sql);
	}

	private List<Map<String, Object>> readAllFromTableUsingSql(String tableName, String sql) {
		try {
			return tryToReadAllFromTable(sql);
		} catch (SqlStorageException e) {
			throw SqlStorageException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private List<Map<String, Object>> tryToReadAllFromTable(String sql) {
		return dataReader.executePreparedStatementQueryUsingSqlAndValues(sql,
				Collections.emptyList());

	}

	private String createSelectAllFor(String tableName) {
		return "select * from " + tableName;
	}

	@Override
	public Map<String, Object> readOneRowFromDbUsingTableAndConditions(String tableName,
			Map<String, Object> conditions) {
		try {
			return tryToReadOneRowFromDbUsingTableAndConditions(tableName, conditions);
		} catch (SqlStorageException e) {
			throw SqlStorageException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private Map<String, Object> tryToReadOneRowFromDbUsingTableAndConditions(String tableName,
			Map<String, Object> conditions) {

		List<Object> values = new ArrayList<>();
		values.addAll(conditions.values());

		String sql = createSqlForTableNameAndConditions(tableName, conditions);
		return dataReader.readOneRowOrFailUsingSqlAndValues(sql, values);
	}

	private String createSqlForTableNameAndConditions(String tableName,
			Map<String, Object> conditions) {
		String sql = "select * from " + tableName + " where ";
		String conditionPart = createConditionPartOfSql(conditions);
		sql += conditionPart;
		return sql;
	}

	private String createConditionPartOfSql(Map<String, Object> conditions) {
		StringJoiner joiner = new StringJoiner(" and ");
		for (String key : conditions.keySet()) {
			joiner.add(key + " = ?");
		}
		return joiner.toString();
	}

	@Override
	public List<Map<String, Object>> readFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		try {
			return tryToReadFromTableUsingConditions(tableName, conditions);
		} catch (SqlStorageException e) {
			throw SqlStorageException.withMessageAndException(ERROR_READING_DATA_FROM + tableName,
					e);
		}
	}

	private List<Map<String, Object>> tryToReadFromTableUsingConditions(String tableName,
			Map<String, Object> conditions) {
		String sql = createSqlForTableNameAndConditions(tableName, conditions);
		List<Object> values = new ArrayList<>();
		values.addAll(conditions.values());
		return dataReader.executePreparedStatementQueryUsingSqlAndValues(sql, values);
	}

	public DataReader getDataReader() {
		// needed for test
		return dataReader;
	}

	@Override
	public Map<String, Object> readNextValueFromSequence(String sequenceName) {
		return dataReader.readOneRowOrFailUsingSqlAndValues(
				"select nextval('" + sequenceName + "')", Collections.emptyList());
	}

	@Override
	public List<Map<String, Object>> readAllFromTable(String tableName,
			ResultDelimiter resultDelimiter) {
		String sql = createSelectAllFor(tableName);
		sql += possiblyCreateDelimiter(resultDelimiter);
		return readAllFromTableUsingSql(tableName, sql);
	}

	private String possiblyCreateDelimiter(ResultDelimiter resultDelimiter) {
		return possiblySetLimit(resultDelimiter) + possiblySetOffset(resultDelimiter);
	}

	private String possiblySetLimit(ResultDelimiter resultDelimiter) {
		return resultDelimiter.limit != null ? " limit " + resultDelimiter.limit : "";
	}

	private String possiblySetOffset(ResultDelimiter resultDelimiter) {
		return resultDelimiter.offset != null ? " offset " + resultDelimiter.offset : "";
	}

	@Override
	public long readNumberOfRows(String tableName, Map<String, Object> conditions) {
		String sql = assembleSqlForNumberOfRows(tableName, conditions);
		List<Object> values = getConditionsAsValues(conditions);
		Map<String, Object> countResult = dataReader.readOneRowOrFailUsingSqlAndValues(sql, values);
		return (long) countResult.get("count");

	}

	private String assembleSqlForNumberOfRows(String type, Map<String, Object> conditions) {
		return "select count(*) from " + type + possiblyAddConditionsToSql(conditions);
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

	@Override
	public long readNumberOfRows(String tableName, Map<String, Object> conditions, Integer fromNo,
			Integer toNo) {
		long numberOfRows = readNumberOfRows(tableName, conditions);
		long maxToNumber = toNoIsNullOrTooLarge(toNo, numberOfRows) ? numberOfRows : toNo;
		long minFromNumber = fromNo < MIN_FROM_NUMBER ? MIN_FROM_NUMBER : fromNo;

		return fromLargerThanTo(minFromNumber, maxToNumber) ? 0
				: calculateDifference(minFromNumber, maxToNumber);
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

}