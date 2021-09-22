/*
 * Copyright 2021 Uppsala University Library
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.TableQuery;

public class TableQueryImp implements TableQuery {

	private String tableName;
	private Map<String, Object> parameters = new LinkedHashMap<>();
	private Map<String, Object> conditions = new LinkedHashMap<>();
	private List<String> orderBy = new ArrayList<>();
	private Long offset;
	private Long lastRecordNumberToShowInQuery;
	private static final String ALLOWED_REGEX = "^[A-Za-z\\-_]*$";
	private static Pattern allowedPattern = Pattern.compile(ALLOWED_REGEX);

	public static TableQueryImp usingTableName(String tableName) {
		throwErrorIfInputContainsForbiddenCharacters(tableName);
		return new TableQueryImp(tableName);
	}

	private static void throwErrorIfInputContainsForbiddenCharacters(String text) {
		if (hasForbiddenCharacters(text)) {
			throw SqlDatabaseException
					.withMessage("Input contains character outside the allowed regexp.");
		}
	}

	private static boolean hasForbiddenCharacters(String text) {
		Matcher allowedMatcher = allowedPattern.matcher(text);
		return !allowedMatcher.matches();
	}

	private TableQueryImp(String tableName) {
		this.tableName = tableName;
	}

	@Override
	public void addParameter(String name, Object value) {
		throwErrorIfInputContainsForbiddenCharacters(name);
		parameters.put(name, value);
	}

	@Override
	public void addCondition(String name, Object value) {
		throwErrorIfInputContainsForbiddenCharacters(name);
		conditions.put(name, value);
	}

	@Override
	public void addOrderByAsc(String column) {
		tryToAddOrderByPart(column, "asc");
	}

	@Override
	public void addOrderByDesc(String column) {
		tryToAddOrderByPart(column, "desc");
	}

	private void tryToAddOrderByPart(String column, String orderDirection) {
		throwErrorIfInputContainsForbiddenCharacters(column);
		this.orderBy.add(column + " " + orderDirection);
	}

	@Override
	public void setFromNo(Long fromNo) {
		this.offset = fromNo - 1;
	}

	@Override
	public void setToNo(Long toNo) {
		this.lastRecordNumberToShowInQuery = toNo;
	}

	@Override
	public String assembleCreateSql() {
		return assembleInsertSql().toString();
	}

	private StringBuilder assembleInsertSql() {
		StringBuilder sql = new StringBuilder("insert into " + tableName + "(");
		sql.append(joinAllFromListAddingToAndSeparatingBy(parameterNames(), "", ", "));
		sql.append(") values(");
		sql.append(addCorrectNumberOfPlaceHoldersForValues(parameterNames()));
		sql.append(')');
		return sql;
	}

	private String addCorrectNumberOfPlaceHoldersForValues(List<String> parameterNames) {
		StringJoiner joiner = new StringJoiner(", ");
		for (int i = 0; i < parameterNames.size(); i++) {
			joiner.add("?");
		}
		return joiner.toString();
	}

	@Override
	public String assembleReadSql() {
		String sql = assembleSelectSql();
		sql += possiblyAddConditionsToSql();
		sql += possiblyAddOrderBy();
		sql += possiblyAddOffsetPart();
		sql += possiblyAddLimitPart();
		return sql;
	}

	private String assembleSelectSql() {
		return "select * from " + tableName;
	}

	private String possiblyAddOrderBy() {
		if (!orderBy.isEmpty()) {
			return " order by " + joinAllFromListAddingToAndSeparatingBy(orderBy, "", ", ");
		}
		return "";
	}

	private String possiblyAddConditionsToSql() {
		if (hasConditions()) {
			return addWherePartOfSqlStatement();
		}
		return "";
	}

	private String addWherePartOfSqlStatement() {
		StringBuilder sql = new StringBuilder(" where ");
		sql.append(joinAllFromListAddingToAndSeparatingBy(condtionNames(), " = ?", " and "));
		return sql.toString();
	}

	private List<String> condtionNames() {
		return new ArrayList<>(conditions.keySet());
	}

	private List<String> parameterNames() {
		return new ArrayList<>(parameters.keySet());
	}

	public boolean hasConditions() {
		return !conditions.isEmpty();
	}

	private String joinAllFromListAddingToAndSeparatingBy(List<String> list, String toAdd,
			String delimiter) {
		StringJoiner joiner = new StringJoiner(delimiter);
		for (String element : list) {
			joiner.add(element + toAdd);
		}
		return joiner.toString();
	}

	private String possiblyAddOffsetPart() {
		if (offset != null) {
			return " offset " + offset;
		}
		return "";
	}

	private String possiblyAddLimitPart() {
		if (lastRecordNumberToShowInQuery != null) {
			return " limit " + calculateLimit();
		}
		return "";
	}

	private String calculateLimit() {
		if (offset == null) {
			return lastRecordNumberToShowInQuery.toString();
		} else {
			Long limit = lastRecordNumberToShowInQuery - offset - 1;
			return limit.toString();
		}
	}

	@Override
	public String assembleUpdateSql() {
		return createUpdateSql();
	}

	private String createUpdateSql() {
		StringBuilder sql = new StringBuilder("update " + tableName + " set ");
		sql.append(joinAllFromListAddingToAndSeparatingBy(parameterNames(), " = ?", ", "));
		sql.append(possiblyAddConditionsToSql());
		return sql.toString();
	}

	@Override
	public String assembleDeleteSql() {
		StringBuilder sql = new StringBuilder("delete from " + tableName);
		sql.append(possiblyAddConditionsToSql());
		return sql.toString();
	}

	@Override
	public List<Object> getQueryValues() {
		List<Object> values = new ArrayList<>();
		values.addAll(parameters.values());
		values.addAll(conditions.values());
		return values;
	}

	@Override
	public String assembleCountSql() {
		return "select count (*) from (" + assembleReadSql() + ") as count";
	}
}
