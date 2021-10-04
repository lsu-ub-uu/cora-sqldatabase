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
import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;
import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;

/**
 * SqlDatabaseFactoryImp implements {@link SqlDatabaseFactory}. To get an instance of this class
 * look at {@link #usingLookupNameFromContext(String)} and
 * {@link #usingUriAndUserAndPassword(String, String, String)} respectively.
 * <p>
 * SqlDatabaseFactoryImp is threadsafe
 */
public class SqlDatabaseFactoryImp implements SqlDatabaseFactory {
	private SqlConnectionProvider sqlConnectionProvider;
	private String lookupName;
	private String url;
	private String user;
	private String password;

	/**
	 * usingLookupNameFromContext creates a new instance of this class that uses the provided
	 * lookupName to find connection details for the database in the context.
	 * 
	 * @param lookupName
	 *            A String with the lookup name to use to find connection details for the database
	 *            from the context.
	 * @return A new instance of SqlDatabaseFactoryImp
	 */
	public static SqlDatabaseFactoryImp usingLookupNameFromContext(String lookupName) {
		return new SqlDatabaseFactoryImp(lookupName);
	}

	SqlDatabaseFactoryImp(String lookupName) {
		// package private for test reasons
		this.lookupName = lookupName;
	}

	/**
	 * usingUriAndUserAndPassword creates a new instance of this class that uses the provided url,
	 * user and password as connection details for the database.
	 * 
	 * @param url
	 *            A String with url to the database
	 * @param user
	 *            A String with the username
	 * @param password
	 *            A String with the password
	 * @return A new instance of SqlDatabaseFactoryImp
	 */
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
	public TableFacade factorTableFacade() {
		DatabaseFacade dbFacade = factorDatabaseFacade();
		return TableFacadeImp.usingDatabaseFacade(dbFacade);
	}

	@Override
	public DatabaseFacade factorDatabaseFacade() {
		createConnectionProviderIfNotCreatedSinceBefore();
		return DatabaseFacadeImp.usingSqlConnectionProvider(sqlConnectionProvider);
	}

	private synchronized void createConnectionProviderIfNotCreatedSinceBefore() {
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
		return null != lookupName;
	}

	void createContextConnectionProvider() throws NamingException {
		InitialContext context = new InitialContext();
		sqlConnectionProvider = ContextConnectionProviderImp.usingInitialContextAndName(context,
				lookupName);
	}

	private void createParameterConnectionProvider() {
		sqlConnectionProvider = ParameterConnectionProviderImp.usingUriAndUserAndPassword(url, user,
				password);
	}

	SqlConnectionProvider getSqlConnectionProvider() {
		// needed for tests
		return sqlConnectionProvider;
	}

	@Override
	public TableQuery factorTableQuery(String tableName) {
		return TableQueryImp.usingTableName(tableName);
	}

	public String onlyForTestGetLookupName() {
		return lookupName;
	}

}
