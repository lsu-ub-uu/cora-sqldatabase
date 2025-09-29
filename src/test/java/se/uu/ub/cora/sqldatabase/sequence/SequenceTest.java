/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.sequence;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertTrue;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.sequence.internal.SequenceImp;

public class SequenceTest {

	private SequenceImp sequence;
	private SqlConnectionProviderSpy sqlConnectionProvider;

	@BeforeMethod
	private void beforeMethod() {
		sqlConnectionProvider = new SqlConnectionProviderSpy();
		sequence = SequenceImp.usingSqlConnectionProvider(sqlConnectionProvider);
	}

	@Test
	public void testDatabaseFacadeExtendsAutoclosable() {
		assertTrue(sequence instanceof SequenceImp);
		assertTrue(sequence instanceof AutoCloseable);
	}

	@Test
	public void testCreateAndGetNextValueForSequence() {
		sequence.createSequence("someSequence", 1);
		long nextValue = sequence.getNextValueForSequence("someSequence");
		assertEquals(nextValue, 1);
	}

	@Test
	public void testOnlyForTests() {
		assertEquals(sequence.onlyForTestGetSqlConnectionProvider(), sqlConnectionProvider);
	}
}
