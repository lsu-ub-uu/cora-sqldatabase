/*
 * Copyright 2018, 2019, 2021, 2026 Uppsala University Library
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

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
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
import se.uu.ub.cora.sqldatabase.sequence.DatabaseFacadeSpy;
import se.uu.ub.cora.sqldatabase.sequence.internal.SequenceImp;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;
import se.uu.ub.cora.sqldatabase.table.internal.TableQueryImp;
import se.uu.ub.cora.testspies.logger.LoggerFactorySpy;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;
import se.uu.ub.cora.testutils.mrv.MethodReturnValues;

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
	public void testInitFromContextError() {
		SqlDatabaseFactory tableFacadeFactory = new SqlDatabaseFactoryImpForThrowErrorInsteadOfCreatingContext();
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
	public void testInitFromContext() {
		assertEquals(sqlDatabaseFactory.onlyForTestGetLookupName(), lookupName);
	}

	@Test
	public void testConnectionProvider() {
		sqlDatabaseFactory.factorTableFacade();

		ContextConnectionProviderImp sqlConnectionProvider = (ContextConnectionProviderImp) sqlDatabaseFactory
				.getSqlConnectionProvider();
		assertTrue(sqlConnectionProvider.getContext() instanceof InitialContext);
		assertEquals(sqlConnectionProvider.getName(), lookupName);
	}

	@Test
	public void testInitFromUriUserPassword() {
		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(url, user, password);
		assertEquals(sqlDatabaseFactory.onlyForTestGetUrl(), url);
		assertEquals(sqlDatabaseFactory.onlyForTestGetUser(), user);
		assertEquals(sqlDatabaseFactory.onlyForTestGetPassword(), password);
	}

	@Test
	public void testFactorTableFacadeFromUriUserPassword() throws Exception {
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
	public void testFactorTableFacade() {
		TableFacade tableFacade = sqlDatabaseFactory.factorTableFacade();
		assertTrue(tableFacade instanceof TableFacadeImp);
	}

	@Test
	public void testTwoCallsToFactoryReturnsDifferentInstances() {
		TableFacade tableFacade = sqlDatabaseFactory.factorTableFacade();
		TableFacade tableFacade2 = sqlDatabaseFactory.factorTableFacade();
		assertNotSame(tableFacade, tableFacade2);
	}

	@Test
	public void testWhenInitFromContextFactorMoreThanOnceUsesSameConnectionProvider() {
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
		return databaseFacade.getSqlConnectionProvider();
	}

	@Test
	public void testWhenInitFromParametersFactorMoreThanOnceUsesSameConnectionProvider() {
		sqlDatabaseFactory = SqlDatabaseFactoryImp.usingUriAndUserAndPassword(url, user, password);
		ensureSameConnectionProviderForTwoFactoryCalls();
	}

	@Test
	public void testDataReaderSetWithDependencesInRecordReader() {
		SqlDatabaseFactoryImp tableFacadeFactory = SqlDatabaseFactoryImp
				.usingLookupNameFromContext("someName");

		TableFacadeImp tableFacade = (TableFacadeImp) tableFacadeFactory.factorTableFacade();

		DatabaseFacadeImp databaseFacade = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();
		assertSame(databaseFacade.getSqlConnectionProvider(),
				tableFacadeFactory.getSqlConnectionProvider());
	}

	@Test
	public void testFactorDatabaseFacade() {
		DatabaseFacade dbFacade = sqlDatabaseFactory.factorDatabaseFacade();
		assertTrue(dbFacade instanceof DatabaseFacadeImp);
	}

	@Test
	public void testTwoCallsToFactoryDatabaseReturnsDifferentInstances() {
		DatabaseFacade databaseFacade = sqlDatabaseFactory.factorDatabaseFacade();
		DatabaseFacade databaseFacade2 = sqlDatabaseFactory.factorDatabaseFacade();
		assertNotSame(databaseFacade, databaseFacade2);
	}

	@Test
	public void testFactoriesUseSameSqlConnectionProvider() {
		DatabaseFacadeImp dbFacade = (DatabaseFacadeImp) sqlDatabaseFactory.factorDatabaseFacade();
		TableFacadeImp tableFacade = (TableFacadeImp) sqlDatabaseFactory.factorTableFacade();
		DatabaseFacadeImp databaseFacade = (DatabaseFacadeImp) tableFacade.getDatabaseFacade();

		assertSame(dbFacade.getSqlConnectionProvider(), databaseFacade.getSqlConnectionProvider());

	}

	@Test
	public void testThreadsWhenCreatingConnectionProvider() throws Exception {
		Method declaredMethod = SqlDatabaseFactoryImp.class
				.getDeclaredMethod("createConnectionProviderIfNotCreatedSinceBefore");
		assertTrue(Modifier.isSynchronized(declaredMethod.getModifiers()));

	}

	@Test
	public void testFactorTableQuery() {
		String tableName = "someTableName";
		TableQuery tableQuery = sqlDatabaseFactory.factorTableQuery(tableName);
		assertTrue(tableQuery instanceof TableQueryImp);
	}

	@Test
	public void testFactorTableQueryNameIsSet() {
		String tableName = "someTableName";
		TableQueryImp tableQuery = (TableQueryImp) sqlDatabaseFactory.factorTableQuery(tableName);
		assertEquals(tableQuery.getTableName(), tableName);
	}

	@Test
	public void testFactorSequence() {
		SqlDatabaseFactoryWithMCR sqlDatabaseFactoryWithMCR = new SqlDatabaseFactoryWithMCR();

		SequenceImp sequence = (SequenceImp) sqlDatabaseFactoryWithMCR.factorSequence();

		var databaseFacade = sqlDatabaseFactoryWithMCR.MCR
				.assertCalledParametersReturn("factorDatabaseFacade");
		assertEquals(sequence.onlyForTestGetDatabaseFacade(), databaseFacade);
	}

	@Test
	public void testOnlyForTestGetLookupName() {
		assertEquals(sqlDatabaseFactory.onlyForTestGetLookupName(), lookupName);
	}
}

class SqlDatabaseFactoryWithMCR extends SqlDatabaseFactoryImp {

	public MethodCallRecorder MCR = new MethodCallRecorder();
	public MethodReturnValues MRV = new MethodReturnValues();

	public SqlDatabaseFactoryWithMCR() {
		super("Not important lookup name");
		MCR.useMRV(MRV);
		MRV.setDefaultReturnValuesSupplier("factorDatabaseFacade", DatabaseFacadeSpy::new);
	}

	@Override
	public DatabaseFacade factorDatabaseFacade() {
		return (DatabaseFacade) MCR.addCallAndReturnFromMRV();
	}

}

class SqlDatabaseFactoryImpForThrowErrorInsteadOfCreatingContext extends SqlDatabaseFactoryImp {
	SqlDatabaseFactoryImpForThrowErrorInsteadOfCreatingContext() {
		super("Not important lookup name");
	}

	@Override
	void createContextConnectionProvider() {
		throw SqlDatabaseException.withMessage("Error from overriding test class");
	}

}
