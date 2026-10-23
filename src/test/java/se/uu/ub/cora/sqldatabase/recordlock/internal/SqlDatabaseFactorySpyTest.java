/*
 * Copyright 2026 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.recordlock.internal;

import static org.testng.Assert.assertSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.DatabaseFacade;
import se.uu.ub.cora.sqldatabase.sequence.DatabaseFacadeSpy;
import se.uu.ub.cora.sqldatabase.sequence.Sequence;
import se.uu.ub.cora.sqldatabase.table.TableFacade;
import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.sqldatabase.table.internal.TableQuerySpy;

public class SqlDatabaseFactorySpyTest {
	private SqlDatabaseFactorySpy factory;

	@BeforeMethod
	public void beforeMethod() {
		factory = new SqlDatabaseFactorySpy();
	}

	@Test
	public void testFactorDatabaseFacadeDefaultReturnsNewSpy() {
		DatabaseFacade databaseFacade = factory.factorDatabaseFacade();

		assertTrue(databaseFacade instanceof DatabaseFacadeSpy);
		factory.MCR.assertReturn("factorDatabaseFacade", 0, databaseFacade);
	}

	@Test
	public void testFactorDatabaseFacadeCanThrowException() {
		RuntimeException error = new RuntimeException("someError");
		factory.MRV.setAlwaysThrowException("factorDatabaseFacade", error);

		try {
			factory.factorDatabaseFacade();
			fail();
		} catch (Exception e) {
			assertSame(e, error);
		}
	}

	@Test
	public void testFactorTableFacade() {
		factory.MRV.setDefaultReturnValuesSupplier("factorTableFacade", () -> null);

		TableFacade tableFacade = factory.factorTableFacade();

		factory.MCR.assertReturn("factorTableFacade", 0, tableFacade);
	}

	@Test
	public void testFactorTableQuery() {
		TableQuerySpy tableQuery = new TableQuerySpy();
		factory.MRV.setDefaultReturnValuesSupplier("factorTableQuery", () -> tableQuery);

		TableQuery returned = factory.factorTableQuery("someTable");

		factory.MCR.assertParameters("factorTableQuery", 0, "someTable");
		assertSame(returned, tableQuery);
	}

	@Test
	public void testFactorSequence() {
		factory.MRV.setDefaultReturnValuesSupplier("factorSequence", () -> null);

		Sequence sequence = factory.factorSequence();

		factory.MCR.assertReturn("factorSequence", 0, sequence);
	}
}
