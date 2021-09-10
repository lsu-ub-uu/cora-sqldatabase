/*
 * Copyright 2018, 2019, 2021 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.record;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.connection.ContextConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.data.DataReader;
import se.uu.ub.cora.sqldatabase.data.DataReaderImp;
import se.uu.ub.cora.sqldatabase.record.internal.RecordReaderImp;

public class RecordReaderFactoryImp implements RecordReaderFactory {
	private SqlConnectionProvider sqlConnectionProvider;
	private String name;

	public static RecordReaderFactoryImp usingLookupNameFromContext(String name) {
		return new RecordReaderFactoryImp(name);
	}

	RecordReaderFactoryImp(String name) {
		// package private for test reasons
		this.name = name;
	}

	@Override
	public RecordReader factor() {
		createConnectionProviderIfNotCreatedSinceBefore();
		DataReader dataReader = DataReaderImp.usingSqlConnectionProvider(sqlConnectionProvider);
		return RecordReaderImp.usingDataReader(dataReader);
	}

	private void createConnectionProviderIfNotCreatedSinceBefore() {
		try {
			tryToCreateConnectionProviderIfNotCreatedSinceBefore();
		} catch (Exception e) {
			throw SqlDatabaseException.withMessageAndException(e.getMessage(), e);
		}
	}

	private void tryToCreateConnectionProviderIfNotCreatedSinceBefore() throws NamingException {
		// if (null != name) {
		InitialContext context = new InitialContext();
		createContextConnectionProvider(name, context);
		// }
	}

	void createContextConnectionProvider(String name, InitialContext context) {
		sqlConnectionProvider = ContextConnectionProviderImp.usingInitialContextAndName(context,
				name);
	}

	SqlConnectionProvider getSqlConnectionProvider() {
		// needed for tests
		return sqlConnectionProvider;
	}

}
