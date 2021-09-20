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

package se.uu.ub.cora.sqldatabase.table;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.naming.InitialContext;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.connection.ContextConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.DriverSpy;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.data.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;

public class TableFacadeFactoryTest {
	private TableFacadeFactoryImp tableFacadeFactory;
	private LoggerFactorySpy loggerFactorySpy;
	private String lookupName;
	private DriverSpy driver;
	private String url = "someUrl";
	private String user = "someUser";
	private String password = "somePassword";

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		lookupName = "someLookupName";

		tableFacadeFactory = TableFacadeFactoryImp.usingLookupNameFromContext(lookupName);
	}

	@Test
	public void testInitFromContextError() throws Exception {
		TableFacadeFactory tableFacadeFactory = new RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext();
		Exception error = null;
		try {
			tableFacadeFactory.factor();
		} catch (Exception e) {
			error = e;
		}
		assertNotNull(error);
		assertTrue(error instanceof SqlDatabaseException);
		assertEquals(error.getMessage(), "Error from overriding test class");
		assertTrue(error.getCause() instanceof SqlDatabaseException);
	}

	@Test
	public void testInitFromContext() throws Exception {
		tableFacadeFactory.factor();

		ContextConnectionProviderImp sqlConnectionProvider = (ContextConnectionProviderImp) tableFacadeFactory
				.getSqlConnectionProvider();
		assertTrue(sqlConnectionProvider.getContext() instanceof InitialContext);
		assertEquals(sqlConnectionProvider.getName(), lookupName);
	}

	@Test
	public void testInitFromUriUserPassword() throws Exception {
		driver = new DriverSpy();
		DriverManager.registerDriver(driver);
		tableFacadeFactory = TableFacadeFactoryImp.usingUriAndUserAndPassword(url, user, password);

		tableFacadeFactory.factor();

		ParameterConnectionProviderImp parameterConnectionProvider = (ParameterConnectionProviderImp) tableFacadeFactory
				.getSqlConnectionProvider();
		Connection connection = parameterConnectionProvider.getConnection();
		assertEquals(connection, driver.connectionSpy);
		assertEquals(driver.url, url);
		assertEquals(driver.info.getProperty("user"), user);
		assertEquals(driver.info.getProperty("password"), password);
		DriverManager.deregisterDriver(driver);
	}

	@Test
	public void testFactor() throws Exception {
		TableFacade tableFacade = tableFacadeFactory.factor();
		assertTrue(tableFacade instanceof TableFacadeImp);
	}

	@Test
	public void testWhenInitFromContextFactorMoreThanOnceUsesSameConnectionProvider()
			throws Exception {
		tableFacadeFactory = TableFacadeFactoryImp.usingLookupNameFromContext(lookupName);
		ensureSameConnectionProviderForTwoFactoryCallse();
	}

	private void ensureSameConnectionProviderForTwoFactoryCallse() {
		assertSame(factorTableFacadeAndGetSqlConnectionProviderFromIt(),
				factorTableFacadeAndGetSqlConnectionProviderFromIt());
	}

	private SqlConnectionProvider factorTableFacadeAndGetSqlConnectionProviderFromIt() {
		TableFacadeImp tableFacade = (TableFacadeImp) tableFacadeFactory.factor();
		DatabaseFacadeImp databaseFacade = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();
		SqlConnectionProvider sqlConnectionProvider = databaseFacade.getSqlConnectionProvider();
		return sqlConnectionProvider;
	}

	@Test
	public void testWhenInitFromParametersFactorMoreThanOnceUsesSameConnectionProvider()
			throws Exception {
		tableFacadeFactory = TableFacadeFactoryImp.usingUriAndUserAndPassword(url, user, password);
		ensureSameConnectionProviderForTwoFactoryCallse();
	}

	@Test
	public void testDataReaderSetWithDependencesInRecordReader() throws Exception {
		TableFacadeFactoryImp tableFacadeFactory = TableFacadeFactoryImp
				.usingLookupNameFromContext("someName");

		TableFacadeImp tableFacade = (TableFacadeImp) tableFacadeFactory.factor();

		DatabaseFacadeImp dataReader = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();
		assertSame(dataReader.getSqlConnectionProvider(),
				tableFacadeFactory.getSqlConnectionProvider());
	}
}

class RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext extends TableFacadeFactoryImp {
	RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext() {
		super("Not important lookup name");
	}

	@Override
	void createContextConnectionProvider() {
		throw SqlDatabaseException.withMessage("Error from overriding test class");
	}
}