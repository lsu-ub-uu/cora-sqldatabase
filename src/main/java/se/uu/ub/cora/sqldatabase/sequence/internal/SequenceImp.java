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

import java.text.MessageFormat;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
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
		String createStatement = "create sequence {0} start with {1};";
		String sqlStatementWithValues = MessageFormat.format(createStatement, sequenceName,
				startValue);
		databaseFacade.executeSql(sqlStatementWithValues);
	}

	@Override
	public long getCurrentValueForSequence(String sequenceName) {
		// select currval('mySequence');
		return 0;
	}

	@Override
	public long getNextValueForSequence(String sequenceName) {
		// select nextval('public.mySequence');
		return 1;
	}

	@Override
	public void resetSequenceValue(String sequenceName, long value) {
		// alter sequence mySequence restart with 784;

	}

	@Override
	public void removeSequence(String sequenceName) {
		// drop sequence if exists mySequence;

	}

	public DatabaseFacade onlyForTestGetDatabaseFacade() {
		return databaseFacade;
	}

}
