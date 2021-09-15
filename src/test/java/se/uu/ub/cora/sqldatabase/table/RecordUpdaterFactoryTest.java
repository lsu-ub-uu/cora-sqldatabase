/*
 * Copyright 2019 Uppsala University Library
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

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.SqlConnectionProviderSpy;
import se.uu.ub.cora.sqldatabase.data.DataUpdaterImp;
import se.uu.ub.cora.sqldatabase.table.internal.RecordUpdaterImp;

public class RecordUpdaterFactoryTest {
	private SqlConnectionProviderSpy connectionProvider;
	private RecordUpdaterFactoryImp factory;

	@BeforeMethod
	public void setUp() {
		connectionProvider = new SqlConnectionProviderSpy();
		factory = RecordUpdaterFactoryImp.usingSqlConnectionProvider(connectionProvider);
	}

	@Test
	public void testFactor() {
		RecordUpdater updater = factory.factor();
		assertTrue(updater instanceof RecordUpdaterImp);

	}

	@Test
	public void testCorrectDataUpdaterSentToRecordUpdater() {
		RecordUpdaterImp recordUpdater = (RecordUpdaterImp) factory.factor();
		DataUpdaterImp dataUpdater = (DataUpdaterImp) recordUpdater.getDataUpdater();
		assertSame(dataUpdater.getSqlConnectionProvider(), connectionProvider);

	}

	@Test
	public void testSqlConnectionProvider() {
		assertSame(factory.getSqlConnectionProvider(), connectionProvider);
	}
}
