/*
 * Copyright 2025, 2026 Uppsala University Library
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

import java.util.Collections;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.sequence.internal.SequenceImp;

public class SequenceTest {

	private static final int START_VALUE = 1;
	private static final String SEQUENCE_NAME = "someSequence";
	private Sequence sequence;
	private DatabaseFacadeSpy databaseFacade;

	@BeforeMethod
	private void beforeMethod() {
		databaseFacade = new DatabaseFacadeSpy();
		sequence = SequenceImp.usingDatabaseFacade(databaseFacade);
	}

	@Test
	public void testDatabaseFacadeExtendsAutoclosable() {
		assertTrue(sequence instanceof SequenceImp);
	}

	// TODO handle exceptions
	@Test
	public void testCreateSequence() {
		setReturnValueInRowByName("nextval", 5);

		sequence.createSequence(SEQUENCE_NAME, START_VALUE);

		assertSequenceCreated();
		assertSequenceNextValue();
	}

	private void assertSequenceCreated() {
		String createStatement = "create sequence " + SEQUENCE_NAME + " start with " + START_VALUE
				+ ";";
		databaseFacade.MCR.assertParameters("executeSql", 0, createStatement);
	}

	private void assertSequenceNextValue() {
		String nextValue = "select nextval('" + SEQUENCE_NAME + "');";
		databaseFacade.MCR.assertCalledParameters("readOneRowOrFailUsingSqlAndValues", nextValue,
				Collections.emptyList());
	}

	@Test
	public void testGetCurrentValueForSequence_SequenceDoNotExists() {
		setReturnValueInRowByName("currval", 5);

		long currentValue = sequence.getCurrentValueForSequence(SEQUENCE_NAME);

		databaseFacade.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0,
				"select currval('" + SEQUENCE_NAME + "');", Collections.emptyList());

		assertEquals(currentValue, 5);
	}

	@Test
	public void testGetBextValueForSequence() {
		setReturnValueInRowByName("nextval", 5);

		long nextId = sequence.getNextValueForSequence(SEQUENCE_NAME);

		databaseFacade.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0,
				"select nextval('" + SEQUENCE_NAME + "');", Collections.emptyList());

		assertEquals(nextId, 5);

	}

	private void setReturnValueInRowByName(String columnName, int value) {
		RowSpy row = new RowSpy();
		row.MRV.setSpecificReturnValuesSupplier("getValueByColumn", () -> Long.valueOf(value),
				columnName);
		databaseFacade.MRV.setDefaultReturnValuesSupplier("readOneRowOrFailUsingSqlAndValues",
				() -> row);
	}

	@Test
	public void testUpdateSequenceValue() {
		long newValue = 100;

		sequence.updateSequenceValue(SEQUENCE_NAME, newValue);

		databaseFacade.MCR.assertParameters("executeSql", 0,
				"alter sequence " + SEQUENCE_NAME + " restart with " + newValue + ";");
	}

	@Test
	public void test_RemoveSequence() {
		sequence.removeSequence(SEQUENCE_NAME);

		databaseFacade.MCR.assertParameters("executeSql", 0,
				"drop sequence if exists " + SEQUENCE_NAME + ";");

	}

	@Test
	public void testOnlyForTests() {
		SequenceImp sequenceImp = (SequenceImp) sequence;
		assertEquals(sequenceImp.onlyForTestGetDatabaseFacade(), databaseFacade);
	}

	// private SqlDatabaseFactoryImp createDatabaseFactoryForSystemOne() {
	// return SqlDatabaseFactoryImp.usingUriAndUserAndPassword(
	// "jdbc:postgresql://systemone-postgresql:5432/systemone", "systemone", "systemone");
	// }

	// @Test(enabled = false)
	// private void realTestCreateSequence() {
	// LoggerProvider.setLoggerFactory(new LoggerFactorySpy());
	//
	// SqlDatabaseFactoryImp databaseFactory = createDatabaseFactoryForSystemOne();
	// DatabaseFacadeImp databaseFacadeImp = (DatabaseFacadeImp) databaseFactory
	// .factorDatabaseFacade();
	// String name = "anotherGeneratorssss";
	//
	// // CREATE sequence
	// String createSequence = "create sequence " + name + " start with 526;";
	// databaseFacadeImp.executeSql(createSequence);
	//
	// // NEXTVALUE sequence
	// String readSequence = "select nextval('public." + name + "');";
	// List<Row> result = databaseFacadeImp.readUsingSqlAndValues(readSequence,
	// Collections.emptyList());
	//
	// for (Row row : result) {
	// System.out.println(row.columnSet());
	// System.out.println(row.getValueByColumn("nextval"));
	// }
	//
	// // DELETE sequence
	// String deleteSequence = "drop sequence if exists " + name + ";";
	// databaseFacadeImp.executeSql(deleteSequence);
	// }
}
