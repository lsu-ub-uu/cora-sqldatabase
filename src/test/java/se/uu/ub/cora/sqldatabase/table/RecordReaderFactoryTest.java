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
import se.uu.ub.cora.sqldatabase.data.DataReaderImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;
import se.uu.ub.cora.sqldatabase.table.internal.TableFacadeImp;

public class RecordReaderFactoryTest {
	private RecordReaderFactoryImp readerFactory;
	private LoggerFactorySpy loggerFactorySpy;
	private String lookupName;
	private DriverSpy driver;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		lookupName = "someLookupName";

		readerFactory = RecordReaderFactoryImp.usingLookupNameFromContext(lookupName);
	}

	@Test
	public void testInitFromContextError() throws Exception {
		RecordReaderFactory readerFactory = new RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext();
		Exception error = null;
		try {
			readerFactory.factor();
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
		readerFactory.factor();

		ContextConnectionProviderImp sqlConnectionProvider = (ContextConnectionProviderImp) readerFactory
				.getSqlConnectionProvider();
		assertTrue(sqlConnectionProvider.getContext() instanceof InitialContext);
		assertEquals(sqlConnectionProvider.getName(), lookupName);
	}

	@Test
	public void testInitFromUriUserPassword() throws Exception {
		driver = new DriverSpy();
		DriverManager.registerDriver(driver);
		String url = "";
		String user = "";
		String password = "";
		readerFactory = RecordReaderFactoryImp.usingUriAndUserAndPassword(url, user, password);

		readerFactory.factor();

		ParameterConnectionProviderImp parameterConnectionProvider = (ParameterConnectionProviderImp) readerFactory
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
		TableFacade recordReader = readerFactory.factor();
		assertTrue(recordReader instanceof TableFacadeImp);
	}

	@Test
	public void testDataReaderSetWithDependencesInRecordReader() throws Exception {
		RecordReaderFactoryImp readerFactory = RecordReaderFactoryImp
				.usingLookupNameFromContext("someName");
		readerFactory.factor();
		TableFacadeImp recordReader = (TableFacadeImp) readerFactory.factor();
		DataReaderImp dataReader = (DataReaderImp) recordReader.getDataReader();
		assertSame(dataReader.getSqlConnectionProvider(), readerFactory.getSqlConnectionProvider());
	}
}

class RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext extends RecordReaderFactoryImp {
	RecordReaderFactoryImpForThrowErrorInsteadOfCreatingContext() {
		super("Not important lookup name");
	}

	@Override
	void createContextConnectionProvider(String name, InitialContext context) {
		throw SqlDatabaseException.withMessage("Error from overriding test class");
	}
}