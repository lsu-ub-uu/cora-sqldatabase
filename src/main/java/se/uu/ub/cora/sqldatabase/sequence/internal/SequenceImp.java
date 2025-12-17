/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.sequence.internal;

import java.util.Collections;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.sequence.Sequence;

public class SequenceImp implements Sequence {

	private static final String CREATE_SEQUENCE = "create sequence %s start with %s;";
	private static final String CURRENT_VALUE = "select currval('%s');";
	private static final String NEXTVAL_IN_SEQUENCE = "select nextval('public.%s');";
	private static final String DROP_SEQUENCE = "drop sequence if exists %s;";
	private DatabaseFacade databaseFacade;

	public static SequenceImp usingDatabaseFacade(DatabaseFacade databaseFacade) {
		return new SequenceImp(databaseFacade);
	}

	private SequenceImp(DatabaseFacade databaseFacade) {
		this.databaseFacade = databaseFacade;
	}

	@Override
	public void createSequence(String sequenceName, long startValue) {
		createSequenceInStorage(sequenceName, startValue);
		nextValueForSequence(sequenceName);
	}

	private void nextValueForSequence(String sequenceName) {
		String statement = String.format(NEXTVAL_IN_SEQUENCE, sequenceName);
		databaseFacade.readUsingSqlAndValues(statement, Collections.emptyList());
	}

	private void createSequenceInStorage(String sequenceName, long startValue) {
		String sqlStatementWithValues = String.format(CREATE_SEQUENCE, sequenceName, startValue);
		databaseFacade.executeSql(sqlStatementWithValues);
	}

	@Override
	public long getCurrentValueForSequence(String sequenceName) {
		String expectedSql = String.format(CURRENT_VALUE, sequenceName);
		databaseFacade.readUsingSqlAndValues(expectedSql, Collections.emptyList());
		return 0;
	}

	@Override
	public long getNextValueForSequence(String sequenceName) {
		// select nextval('public.mySequence');

		// String readSequence = "select nextval('public." + name + "');";
		// List<Row> result = databaseFacadeImp.readUsingSqlAndValues(readSequence,
		// Collections.emptyList());
		//
		//
		//
		// for (Row row : result) {
		// System.out.println(row.columnSet());
		// System.out.println(row.getValueByColumn("nextval"));
		// }
		//
		return 1;
	}

	@Override
	public void resetSequenceValue(String sequenceName, long value) {
		// alter sequence mySequence restart with 784;

	}

	@Override
	public void removeSequence(String sequenceName) {
		String statement = String.format(DROP_SEQUENCE, sequenceName);
		databaseFacade.executeSql(statement);
	}

	public DatabaseFacade onlyForTestGetDatabaseFacade() {
		return databaseFacade;
	}

}
