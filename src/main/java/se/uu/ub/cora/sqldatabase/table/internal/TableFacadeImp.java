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

import java.util.Collections;
import java.util.List;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.TableQuery;

public final class TableFacadeImp implements TableFacade {
	private DatabaseFacade dbFacade;
	private static final String NEXTVAL_COLUMN_NAME = "nextval";

	private TableFacadeImp(DatabaseFacade databaseFacade) {
		this.dbFacade = databaseFacade;
	}

	public static TableFacadeImp usingDatabaseFacade(DatabaseFacade dbFacade) {
		return new TableFacadeImp(dbFacade);
	}

	@Override
	public void insertRowUsingQuery(TableQuery tableQuery) {
		String sql = tableQuery.assembleCreateSql();
		List<Object> values = tableQuery.getQueryValues();
		try {
			dbFacade.executeSqlWithValues(sql, values);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error inserting row using sql: " + sql, e);
		}
	}

	@Override
	public Row readOneRowForQuery(TableQuery tableQuery) {
		String sql = tableQuery.assembleReadSql();
		List<Object> values = tableQuery.getQueryValues();
		try {
			return dbFacade.readOneRowOrFailUsingSqlAndValues(sql, values);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error reading one row using sql: " + sql, e);
		}
	}

	@Override
	public List<Row> readRowsForQuery(TableQuery tableQuery) {
		return readAllFromTableUsingSql(tableQuery);
	}

	private List<Row> readAllFromTableUsingSql(TableQuery tableQuery) {
		String sql = tableQuery.assembleReadSql();
		List<Object> queryValues = tableQuery.getQueryValues();
		try {
			return dbFacade.readUsingSqlAndValues(sql, queryValues);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error reading data using sql: " + sql, e);
		}
	}

	@Override
	public long readNumberOfRows(TableQuery tableQuery) {
		String sql = tableQuery.assembleCountSql();
		List<Object> values = tableQuery.getQueryValues();
		try {
			Row count = dbFacade.readOneRowOrFailUsingSqlAndValues(sql, values);
			return (long) count.getValueByColumn("count");
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error reading number of rows using sql: " + sql, e);
		}
	}

	@Override
	public void updateRowsUsingQuery(TableQuery tableQuery) {
		String sql = tableQuery.assembleUpdateSql();
		List<Object> values = tableQuery.getQueryValues();
		try {
			dbFacade.executeSqlWithValues(sql, values);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error updating rows using sql: " + sql, e);
		}
	}

	@Override
	public void deleteRowsForQuery(TableQuery tableQuery) {
		String sql = tableQuery.assembleDeleteSql();
		List<Object> values = tableQuery.getQueryValues();
		try {
			dbFacade.executeSqlWithValues(sql, values);
		} catch (SqlDatabaseException e) {
			throw SqlDatabaseException
					.withMessageAndException("Error deleting rows using sql: " + sql, e);
		}
	}

	@Override
	public long nextValueFromSequence(String sequenceName) {
		String statement = "select nextval('" + sequenceName + "') as " + NEXTVAL_COLUMN_NAME;
		Row row = dbFacade.readOneRowOrFailUsingSqlAndValues(statement, Collections.emptyList());
		return (long) row.getValueByColumn(NEXTVAL_COLUMN_NAME);
	}

	public DatabaseFacade getDatabaseFacade() {
		// needed for test
		return dbFacade;
	}

	@Override
	public void close() {
		dbFacade.close();
	}

	@Override
	public void startTransaction() {
		dbFacade.startTransaction();
	}

	@Override
	public void endTransaction() {
		dbFacade.endTransaction();
	}

	@Override
	public void rollback() {
		dbFacade.rollback();
	}

}