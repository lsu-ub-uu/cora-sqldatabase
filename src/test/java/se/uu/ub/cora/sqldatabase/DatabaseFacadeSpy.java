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
package se.uu.ub.cora.sqldatabase;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.sqldatabase.internal.RowImp;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class DatabaseFacadeSpy implements DatabaseFacade {

	public boolean executePreparedStatementQueryUsingSqlAndValuesWasCalled = false;
	public String sql = "";
	public List<Object> values;
	public List<Row> result = new ArrayList<>();
	public boolean throwError = false;
	public boolean readOneRowFromDbUsingTableAndConditionsWasCalled = false;
	public RowImp oneRowResult;

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public boolean throwDuplicatedKeyError = false;
	public int numberOfAffectedRows = 0;

	@Override
	public List<Row> readUsingSqlAndValues(String sql, List<Object> values) {
		MCR.addCall("sql", sql, "values", values);
		this.sql = sql;
		this.values = values;
		executePreparedStatementQueryUsingSqlAndValuesWasCalled = true;
		if (throwError) {
			throw SqlDatabaseException.withMessage(
					"Error from executePreparedStatementQueryUsingSqlAndValues in DatabaseFacadeSpy");
		}
		Row innerResult = createResult();

		result.add(innerResult);
		MCR.addReturned(result);
		return result;
	}

	private RowImp createResult() {
		RowImp innerResult = new RowImp();
		innerResult.addColumnWithValue("id", "someId");
		innerResult.addColumnWithValue("name", "someName");
		return innerResult;
	}

	@Override
	public Row readOneRowOrFailUsingSqlAndValues(String sql, List<Object> values) {
		MCR.addCall("sql", sql, "values", values);
		this.sql = sql;
		this.values = values;
		readOneRowFromDbUsingTableAndConditionsWasCalled = true;
		if (throwError) {
			throw SqlDatabaseException.withMessage(
					"Error from readOneRowOrFailUsingSqlAndValues in DatabaseFacadeSpy");
		}

		oneRowResult = new RowImp();
		if (sql.startsWith("sql for count from spy")) {
			oneRowResult.addColumnWithValue("count", 453L);
		} else if (sql.startsWith("select nextval")) {
			String nextValName = sql.substring(sql.lastIndexOf(" ") + 1, sql.length());
			oneRowResult.addColumnWithValue(nextValName, 438234090L);
		} else {
			oneRowResult = createResult();
		}
		MCR.addReturned(oneRowResult);
		return oneRowResult;
	}

	@Override
	public int executeSqlWithValues(String sql, List<Object> values) {
		MCR.addCall("sql", sql, "values", values);
		if (throwError) {
			throw SqlDatabaseException
					.withMessage("Error from executeSqlWithValues in DatabaseFacadeSpy");
		}
		if (throwDuplicatedKeyError) {
			throw SqlConflictException
					.withMessage("Error from executeSqlWithValues in DatabaseFacadeSpy");
		}

		MCR.addReturned(numberOfAffectedRows);
		return numberOfAffectedRows;
	}

	@Override
	public void close() {
		MCR.addCall();
		if (throwError) {
			throw SqlDatabaseException.withMessage("Error from close in DatabaseFacadeSpy");
		}
	}

	@Override
	public void startTransaction() {
		MCR.addCall();
		if (throwError) {
			throw SqlDatabaseException
					.withMessage("Error from startTransaction in DatabaseFacadeSpy");
		}
	}

	@Override
	public void endTransaction() {
		MCR.addCall();
		if (throwError) {
			throw SqlDatabaseException
					.withMessage("Error from endTransaction in DatabaseFacadeSpy");
		}
	}

	@Override
	public void rollback() {
		MCR.addCall();
		if (throwError) {
			throw SqlDatabaseException.withMessage("Error from rollback in DatabaseFacadeSpy");
		}

	}

}
