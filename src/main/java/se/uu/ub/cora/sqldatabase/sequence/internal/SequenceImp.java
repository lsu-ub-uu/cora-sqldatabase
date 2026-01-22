/*
 * Copyright 2025, 2026 Uppsala University Library
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
import java.util.List;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.sequence.Sequence;

public class SequenceImp implements Sequence {

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
		initializeSequenceToBeAbleToGetNextValueWithoutGettingAnException(sequenceName);
	}

	private long initializeSequenceToBeAbleToGetNextValueWithoutGettingAnException(
			String sequenceName) {
		return getNextValueForSequence(sequenceName);
	}

	private void createSequenceInStorage(String sequenceName, long startValue) {
		String sql = "select cora_create_sequence(?,?,?);";
		List<Object> values = List.of(sequenceName, calculateMinValue(startValue), startValue);
		databaseFacade.readOneRowOrFailUsingSqlAndValues(sql, values);
	}

	private long calculateMinValue(long startValue) {
		if (startValueIsNegative(startValue)) {
			return startValue;
		}
		return 0;
	}

	private boolean startValueIsNegative(long startValue) {
		return startValue < 0;
	}

	private void executeSql(String string, Object... values) {
		String sql = String.format(string, values);
		databaseFacade.executeSql(sql);
	}

	@Override
	public long getNextValueForSequence(String sequenceName) {
		String sql = String.format("select nextval('%s');", sequenceName);
		return readOneRowAndColumnAsLongUsingSql(sql, "nextval");
	}

	private long readOneRowAndColumnAsLongUsingSql(String sql, String column) {
		Row row = databaseFacade.readOneRowOrFailUsingSqlAndValues(sql, Collections.emptyList());
		return (Long) row.getValueByColumn(column);
	}

	@Override
	public long getCurrentValueForSequence(String sequenceName) {
		String sql = String.format("select currval('%s');", sequenceName);
		return readOneRowAndColumnAsLongUsingSql(sql, "currval");
	}

	@Override
	public void updateSequenceValue(String sequenceName, long newValue) {
		executeSql("alter sequence %s restart with %s;", sequenceName, newValue);
	}

	@Override
	public void removeSequence(String sequenceName) {
		executeSql("drop sequence if exists %s;", sequenceName);
	}

	public DatabaseFacade onlyForTestGetDatabaseFacade() {
		return databaseFacade;
	}
}
