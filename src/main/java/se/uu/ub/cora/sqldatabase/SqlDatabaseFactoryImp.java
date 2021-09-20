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

package se.uu.ub.cora.sqldatabase;

import javax.naming.InitialContext;
import javax.naming.NamingException;

import se.uu.ub.cora.sqldatabase.connection.ContextConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;

public class SqlDatabaseFactoryImp implements SqlDatabaseFactory {
	private SqlConnectionProvider sqlConnectionProvider;
	private String name;
	private String url;
	private String user;
	private String password;

	public static SqlDatabaseFactoryImp usingLookupNameFromContext(String name) {
		return new SqlDatabaseFactoryImp(name);
	}

	SqlDatabaseFactoryImp(String name) {
		// package private for test reasons
		this.name = name;
	}

	public static SqlDatabaseFactoryImp usingUriAndUserAndPassword(String url, String user,
			String password) {
		return new SqlDatabaseFactoryImp(url, user, password);
	}

	private SqlDatabaseFactoryImp(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}

	@Override
	public TableFacade factor() {
		createConnectionProviderIfNotCreatedSinceBefore();
		DatabaseFacade dbFacade = DatabaseFacadeImp
				.usingSqlConnectionProvider(sqlConnectionProvider);
		return TableFacadeImp.usingDatabaseFacade(dbFacade);
	}

	private void createConnectionProviderIfNotCreatedSinceBefore() {
		if (connectionProviderNeedsToBeCreated()) {
			createConnectionProvider();
		}
	}

	private boolean connectionProviderNeedsToBeCreated() {
		return null == sqlConnectionProvider;
	}

	private void createConnectionProvider() {
		try {
			tryToCreateConnectionProvider();
		} catch (Exception e) {
			throw SqlDatabaseException.withMessageAndException(e.getMessage(), e);
		}
	}

	private void tryToCreateConnectionProvider() throws NamingException {
		if (connectionInfoIsProvidedInContext()) {
			createContextConnectionProvider();
		} else {
			createParameterConnectionProvider();
		}
	}

	private boolean connectionInfoIsProvidedInContext() {
		return null != name;
	}

	void createContextConnectionProvider() throws NamingException {
		InitialContext context = new InitialContext();
		sqlConnectionProvider = ContextConnectionProviderImp.usingInitialContextAndName(context,
				name);
	}

	private void createParameterConnectionProvider() {
		sqlConnectionProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(url, user,
				password);
	}

	SqlConnectionProvider getSqlConnectionProvider() {
		// needed for tests
		return sqlConnectionProvider;
	}

}
