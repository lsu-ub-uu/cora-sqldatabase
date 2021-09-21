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
import java.util.StringJoiner;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.Conditions;
import se.uu.ub.cora.sqldatabase.table.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.table.Parameters;
import se.uu.ub.cora.sqldatabase.table.TableFacade;

public final class TableFacadeImp implements TableFacade {
	private static final int MIN_FROM_NUMBER = 1;
	private static final String ERROR_READING_DATA_FROM = "Error reading data from ";
	private DatabaseFacade dbFacade;
	private static final String NEXTVAL_COLUMN_NAME = "nextval";

	private TableFacadeImp(DatabaseFacade databaseFacade) {
		this.dbFacade = databaseFacade;
	}

	public static TableFacadeImp usingDatabaseFacade(DatabaseFacade dbFacade) {
		return new TableFacadeImp(dbFacade);
	}

	@Override
	public List<Row> readRowsFromTable(String tableName) {
		String sql = assembleSqlForReadAllFromTable(tableName);
		return readAllFromTableUsingSql(tableName, sql);
	}

	// REMOVE
	private String assembleSqlForReadAllFromTable(String tableName) {
		return "select * from " + tableName;
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
		String sql = createSqlForTableNameAndConditionsNames(tableName, conditions);
		return dbFacade.readOneRowOrFailUsingSqlAndValues(sql, conditions.getValues());
	}

	private String createSqlForTableNameAndConditionsNames(String tableName,
			Conditions conditions) {
		return assembleSqlForReadAllFromTable(tableName) + possiblyAddConditionsToSql(conditions);
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
		String sql = createSqlForTableNameAndConditionsNames(tableName, conditions);
		return dbFacade.readUsingSqlAndValues(sql, conditions.getValues());
	}

	public DatabaseFacade getDatabaseFacade() {
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
		String sql = assembleSqlForReadAllFromTable(tableName);
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
		return "select count(*) from " + tableName + possiblyAddConditionsToSql(conditions);
	}

	private String possiblyAddConditionsToSql(Conditions conditions) {
		if (conditions.hasConditions()) {
			return createWherePartOfSqlStatement(conditions);
		}
		return "";
	}

	private String createWherePartOfSqlStatement(Conditions conditions) {
		StringBuilder sql = new StringBuilder(" where ");
		sql.append(joinAllFromListAddingToAndSeparatingBy(conditions.getNames(), " = ?", " and "));
		return sql.toString();
	}

	private String joinAllFromListAddingToAndSeparatingBy(List<String> list, String toAdd,
			String delimiter) {
		StringJoiner joiner = new StringJoiner(delimiter);
		for (String element : list) {
			joiner.add(element + toAdd);
		}
		return joiner.toString();
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
	public void insertRowInTableWithValues(String tableName, Parameters parameters) {
		StringBuilder sql = createSql(tableName, parameters);
		dbFacade.executeSqlWithValues(sql.toString(), parameters.getValues());

	}

	private StringBuilder createSql(String tableName, Parameters parameters) {
		StringBuilder sql = new StringBuilder("insert into " + tableName + "(");
		sql.append(joinAllFromListAddingToAndSeparatingBy(parameters.getNames(), "", ", "));
		sql.append(") values(");
		sql.append(addCorrectNumberOfPlaceHoldersForValues(parameters.getNames()));
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
	public List<Row> readRowsFromTableUsingConditionsAndQueryInfo(String tableName,
			Conditions conditions, DbQueryInfo queryInfo) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void updateRowInTableUsingValuesAndConditions(String tableName, Parameters parameters,
			Conditions conditions) {
		String sql = createUpdateSql(tableName, parameters, conditions);
		List<Object> valuesForUpdate = getListOfValuesFromParametersAndConditions(parameters,
				conditions);
		dbFacade.executeSqlWithValues(sql, valuesForUpdate);
	}

	private String createUpdateSql(String tableName, Parameters parameters, Conditions conditions) {
		StringBuilder sql = new StringBuilder("update " + tableName + " set ");
		sql.append(joinAllFromListAddingToAndSeparatingBy(parameters.getNames(), " = ?", ", "));
		sql.append(possiblyAddConditionsToSql(conditions));
		return sql.toString();
	}

	private List<Object> getListOfValuesFromParametersAndConditions(Parameters parameters,
			Conditions conditions) {
		List<Object> valuesForUpdate = new ArrayList<>();
		valuesForUpdate.addAll(parameters.getValues());
		valuesForUpdate.addAll(conditions.getValues());
		return valuesForUpdate;
	}

	@Override
	public void deleteRowFromTableUsingConditions(String tableName, Conditions conditions) {
		StringBuilder sql = new StringBuilder("delete from " + tableName);
		sql.append(possiblyAddConditionsToSql(conditions));
		dbFacade.executeSqlWithValues(sql.toString(), conditions.getValues());
	}

}