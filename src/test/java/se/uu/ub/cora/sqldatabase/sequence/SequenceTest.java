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
import java.util.List;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.sequence.internal.SequenceImp;

public class SequenceTest {

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
		assertTrue(sequence instanceof AutoCloseable);
		assertTrue(sequence instanceof SequenceImp);
	}

	@Test
	public void testCloseCallsCloseInDbFacade() {
		sequence.close();
		databaseFacade.MCR.assertMethodWasCalled("close");
	}

	@Test
	public void testCreateSequenceWithCurrentValue0() {
		int someValue = 123;
		setReturnValueInRowByName("nextval", someValue);
		int currentValue = 0;

		sequence.createSequence(SEQUENCE_NAME, currentValue);

		databaseFacade.MCR.assertMethodWasCalled("startTransaction");
		assertSequenceCreatedWithStartAndMinValue(currentValue, 0);
		databaseFacade.MCR.assertMethodWasCalled("endTransaction");
		assertSequenceNextValueCalled();
	}

	@Test
	public void testCreateSequenceWithCurrentValue100() {
		int someValue = 123;
		setReturnValueInRowByName("nextval", someValue);
		int currentValue = 100;

		sequence.createSequence(SEQUENCE_NAME, currentValue);

		assertSequenceCreatedWithStartAndMinValue(currentValue, 0);
		assertSequenceNextValueCalled();
	}

	@Test
	public void testCreateSequenceWithCurrentValueNegative100() {
		int someValue = 123;
		setReturnValueInRowByName("nextval", someValue);
		int currentValue = -100;

		sequence.createSequence(SEQUENCE_NAME, currentValue);

		assertSequenceCreatedWithStartAndMinValue(currentValue, -100);
		assertSequenceNextValueCalled();
	}

	private void assertSequenceCreatedWithStartAndMinValue(long startValue, long sequenceMinValue) {
		String sql = "select cora_create_sequence(?,?,?);";
		databaseFacade.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0, sql);
		databaseFacade.MCR.assertParameterAsEqual("readOneRowOrFailUsingSqlAndValues", 0, "values",
				List.of(SEQUENCE_NAME, sequenceMinValue, startValue));
	}

	private void assertSequenceNextValueCalled() {
		String nextValue = "select nextval('" + SEQUENCE_NAME + "');";
		databaseFacade.MCR.assertCalledParameters("readOneRowOrFailUsingSqlAndValues", nextValue,
				Collections.emptyList());
	}

	@Test
	public void testGetCurrentValueForSequence_SequenceDoNotExists() {
		setReturnValueInRowByName("last_value", 5);

		long currentValue = sequence.getCurrentValueForSequence(SEQUENCE_NAME);

		databaseFacade.MCR.assertParameters("readOneRowOrFailUsingSqlAndValues", 0,
				"select last_value FROM " + SEQUENCE_NAME + ";", Collections.emptyList());

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
		sequence.deleteSequence(SEQUENCE_NAME);

		databaseFacade.MCR.assertParameters("executeSql", 0,
				"drop sequence if exists " + SEQUENCE_NAME + ";");
	}

	@Test
	public void testOnlyForTests() {
		SequenceImp sequenceImp = (SequenceImp) sequence;
		assertEquals(sequenceImp.onlyForTestGetDatabaseFacade(), databaseFacade);
	}
}
