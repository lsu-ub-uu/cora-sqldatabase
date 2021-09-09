/*
 * Copyright 2020 Uppsala University Library
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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.sqldatabase.SqlConnectionProviderSpy;
import se.uu.ub.cora.sqldatabase.data.DataUpdaterImp;
import se.uu.ub.cora.sqldatabase.log.LoggerFactorySpy;
import se.uu.ub.cora.sqldatabase.record.internal.RecordCreatorImp;

public class RecordCreatorFactoryTest {
	private SqlConnectionProviderSpy connectionProvider;
	private RecordCreatorFactory creatorFactory;
	private LoggerFactorySpy loggerFactorySpy;

	@BeforeMethod
	public void beforeMethod() {
		loggerFactorySpy = new LoggerFactorySpy();
		LoggerProvider.setLoggerFactory(loggerFactorySpy);
		connectionProvider = new SqlConnectionProviderSpy();
		creatorFactory = RecordCreatorFactoryImp.usingSqlConnectionProvider(connectionProvider);
	}

	@Test
	public void testInit() throws Exception {
		assertEquals(creatorFactory.getSqlConnectionProvider(), connectionProvider);
	}

	@Test
	public void testFactor() throws Exception {
		RecordCreator recordCreator = creatorFactory.factor();
		assertTrue(recordCreator instanceof RecordCreatorImp);
	}

	@Test
	public void testDataReaderInRecordReader() throws Exception {
		RecordCreatorImp recordCreator = (RecordCreatorImp) creatorFactory.factor();
		DataUpdaterImp dataUpdater = (DataUpdaterImp) recordCreator.getDataUpdater();
		assertSame(dataUpdater.getSqlConnectionProvider(), connectionProvider);
	}
}
