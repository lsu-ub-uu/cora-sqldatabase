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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotNull;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DriverManager;

import javax.naming.InitialContext;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.connection.ContextConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.DriverSpy;
import se.uu.ub.cora.sqldatabase.connection.ParameterConnectionProviderImp;
import se.uu.ub.cora.sqldatabase.connection.SqlConnectionProvider;
import se.uu.ub.cora.sqldatabase.internal.DatabaseFacadeImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;

public class SqlDatabaseFactoryTest {
	private SqlDatabaseFactoryImp sqlDatabaseFactory;
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

		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingLookupNameFromContext(lookupName);
	}

	@Test
	public void testInitFromContextError() throws Exception {
		SqlDatabaseFactory tableFacadeFactory = new RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext();
		Exception error = null;
		try {
			tableFacadeFactory.factorTableFacade();
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
		sqlDatabaseFactory.factorTableFacade();

		ContextConnectionProviderImp sqlConnectionProvider = (ContextConnectionProviderImp) sqlDatabaseFactory
				.getSqlConnectionProvider();
		assertTrue(sqlConnectionProvider.getContext() instanceof InitialContext);
		assertEquals(sqlConnectionProvider.getName(), lookupName);
	}

	@Test
	public void testInitFromUriUserPassword() throws Exception {
		driver = new DriverSpy();
		DriverManager.registerDriver(driver);
		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(url, user, password);

		sqlDatabaseFactory.factorTableFacade();

		ParameterConnectionProviderImp parameterConnectionProvider = (ParameterConnectionProviderImp) sqlDatabaseFactory
				.getSqlConnectionProvider();
		Connection connection = parameterConnectionProvider.getConnection();
		assertEquals(connection, driver.connectionSpy);
		assertEquals(driver.url, url);
		assertEquals(driver.info.getProperty("user"), user);
		assertEquals(driver.info.getProperty("password"), password);
		DriverManager.deregisterDriver(driver);
	}

	@Test
	public void testFactorTableFacade() throws Exception {
		TableFacade tableFacade = sqlDatabaseFactory.factorTableFacade();
		assertTrue(tableFacade instanceof TableFacadeImp);
	}

	@Test
	public void testTwoCallsToFactoryReturnsDifferentInstances() throws Exception {
		TableFacade tableFacade = sqlDatabaseFactory.factorTableFacade();
		TableFacade tableFacade2 = sqlDatabaseFactory.factorTableFacade();
		assertNotSame(tableFacade, tableFacade2);
	}

	@Test
	public void testWhenInitFromContextFactorMoreThanOnceUsesSameConnectionProvider()
			throws Exception {
		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingLookupNameFromContext(lookupName);
		ensureSameConnectionProviderForTwoFactoryCalls();
	}

	private void ensureSameConnectionProviderForTwoFactoryCalls() {
		assertSame(factorTableFacadeAndGetSqlConnectionProviderFromIt(),
				factorTableFacadeAndGetSqlConnectionProviderFromIt());
	}

	private SqlConnectionProvider factorTableFacadeAndGetSqlConnectionProviderFromIt() {
		TableFacadeImp tableFacade = (TableFacadeImp) sqlDatabaseFactory.factorTableFacade();
		DatabaseFacadeImp databaseFacade = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();
		SqlConnectionProvider sqlConnectionProvider = databaseFacade.getSqlConnectionProvider();
		return sqlConnectionProvider;
	}

	@Test
	public void testWhenInitFromParametersFactorMoreThanOnceUsesSameConnectionProvider()
			throws Exception {
		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(url, user, password);
		ensureSameConnectionProviderForTwoFactoryCalls();
	}

	@Test
	public void testDataReaderSetWithDependencesInRecordReader() throws Exception {
		SqlDatabaseFactoryImp tableFacadeFactory = SqlDatabaseFactoryImp
				.usingLookupNameFromContext("someName");

		TableFacadeImp tableFacade = (TableFacadeImp) tableFacadeFactory.factorTableFacade();

		DatabaseFacadeImp dataReader = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();
		assertSame(dataReader.getSqlConnectionProvider(),
				tableFacadeFactory.getSqlConnectionProvider());
	}

	@Test
	public void testFactorDatabaseFacade() throws Exception {
		DatabaseFacade dbFacade = sqlDatabaseFactory.factorDatabaseFacade();
		assertTrue(dbFacade instanceof DatabaseFacadeImp);
	}

	@Test
	public void testTwoCallsToFactoryDatabaseReturnsDifferentInstances() throws Exception {
		DatabaseFacade databaseFacade = sqlDatabaseFactory.factorDatabaseFacade();
		DatabaseFacade databaseFacade2 = sqlDatabaseFactory.factorDatabaseFacade();
		assertNotSame(databaseFacade, databaseFacade2);
	}

	@Test
	public void testFactoriesUseSameSqlConnectionProvider() throws Exception {
		DatabaseFacadeImp dbFacade = (DatabaseFacadeImp) sqlDatabaseFactory.factorDatabaseFacade();
		TableFacadeImp tableFacade = (TableFacadeImp) sqlDatabaseFactory.factorTableFacade();
		DatabaseFacadeImp databaseFacade = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();

		assertSame(dbFacade.getSqlConnectionProvider(), databaseFacade.getSqlConnectionProvider());

	}

	@Test
	public void testName() throws Exception {

	}
}

class RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext extends SqlDatabaseFactoryImp {
	RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext() {
		super("Not important lookup name");
	}

	@Override
	void createContextConnectionProvider() {
		throw SqlDatabaseException.withMessage("Error from overriding test class");
	}
}

class SqlDatabaseFactoryImpForThreadTests extends SqlDatabaseFactoryImp {
	SqlDatabaseFactoryImpForThreadTests() {
		super("Not important lookup name");
	}

	@Override
	void createContextConnectionProvider() {
		throw SqlDatabaseException.withMessage("Error from overriding test class");
	}
}