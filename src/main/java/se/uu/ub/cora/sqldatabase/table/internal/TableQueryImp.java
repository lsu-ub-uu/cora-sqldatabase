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
import java.util.List;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.TableQuery;

public class TableQueryImp implements TableQuery {

	private static final int OFFSET_DIFF = 1;
	private String tableName;
	private List<String> parameterNames = new ArrayList<>();
	private List<Object> parameterValues = new ArrayList<>();
	private List<String> conditionNames = new ArrayList<>();
	private List<Object> conditionValues = new ArrayList<>();
	private List<String> orderBy = new ArrayList<>();
	private Long offset;
	private Long toNumber;
	private static final String ALLOWED_REGEX = "^[.A-Za-z\\-_]*$";
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
		parameterNames.add(name);
		parameterValues.add(value);
	}

	@Override
	public void addCondition(String name, Object value) {
		throwErrorIfInputContainsForbiddenCharacters(name);
		conditionNames.add(name);
		conditionValues.add(value);
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
		this.offset = fromNo - OFFSET_DIFF;
	}

	@Override
	public void setToNo(Long toNo) {
		this.toNumber = toNo;
	}

	@Override
	public String assembleCreateSql() {
		String sql = "insert into " + tableName + "(";
		sql += joinAllFromListAddingToAndSeparatingBy(parameterNames, "", ", ");
		sql += ") values(";
		sql += addPlaceHoldersForParameters();
		sql += ")";
		return sql;
	}

	private String addPlaceHoldersForParameters() {
		StringJoiner joiner = new StringJoiner(", ");
		for (int i = 0; i < parameterNames.size(); i++) {
			joiner.add("?");
		}
		return joiner.toString();
	}

	@Override
	public String assembleReadSql() {
		String sql = "select * from " + tableName;
		sql += possiblyAddConditions();
		sql += possiblyAddOrderBy();
		sql += possiblyAddOffset();
		sql += possiblyAddLimit();
		return sql;
	}

	private String possiblyAddOrderBy() {
		if (!orderBy.isEmpty()) {
			return " order by " + joinAllFromListAddingToAndSeparatingBy(orderBy, "", ", ");
		}
		return "";
	}

	private String possiblyAddConditions() {
		if (hasConditions()) {
			return addWherePartOfSqlStatement();
		}
		return "";
	}

	private String addWherePartOfSqlStatement() {
		StringBuilder sql = new StringBuilder(" where ");
		sql.append(joinAllFromListAddingToAndSeparatingBy(conditionNames, " = ?", " and "));
		return sql.toString();
	}

	public boolean hasConditions() {
		return !conditionNames.isEmpty();
	}

	private String joinAllFromListAddingToAndSeparatingBy(List<String> list, String toAdd,
			String delimiter) {
		StringJoiner joiner = new StringJoiner(delimiter);
		for (String element : list) {
			joiner.add(element + toAdd);
		}
		return joiner.toString();
	}

	private String possiblyAddOffset() {
		if (offsetIsSet()) {
			return " offset " + offset;
		}
		return "";
	}

	private String possiblyAddLimit() {
		if (toNumberIsSet()) {
			return " limit " + calculateLimit();
		}
		return "";
	}

	private boolean toNumberIsSet() {
		return toNumber != null;
	}

	private Long calculateLimit() {
		if (offsetIsSet()) {
			return toNumber - offset;
		}
		return toNumber;
	}

	private boolean offsetIsSet() {
		return offset != null;
	}

	@Override
	public String assembleUpdateSql() {
		return createUpdateSql();
	}

	private String createUpdateSql() {
		String sql = "update " + tableName + " set ";
		sql += joinAllFromListAddingToAndSeparatingBy(parameterNames, " = ?", ", ");
		sql += possiblyAddConditions();
		return sql;
	}

	@Override
	public String assembleDeleteSql() {
		return "delete from " + tableName + possiblyAddConditions();
	}

	@Override
	public List<Object> getQueryValues() {
		List<Object> values = new ArrayList<>();
		values.addAll(parameterValues);
		values.addAll(conditionValues);
		return values;
	}

	@Override
	public String assembleCountSql() {
		return "select count (*) from (" + assembleReadSql() + ") as count";
	}

	public String getTableName() {
		// needed for test
		return tableName;
	}
}
