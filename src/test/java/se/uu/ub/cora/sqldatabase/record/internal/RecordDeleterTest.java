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
package se.uu.ub.cora.sqldatabase.record.internal;

import static org.testng.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.sqldatabase.data.DataUpdater;
import se.uu.ub.cora.sqldatabase.data.DataUpdaterSpy;
import se.uu.ub.cora.sqldatabase.record.RecordDeleter;

public class RecordDeleterTest {

	private DataUpdater dataUpdater;
	private RecordDeleter recordDeleter;
	private Map<String, Object> conditions = new HashMap<>();

	@BeforeMethod
	public void setUp() {
		dataUpdater = new DataUpdaterSpy();
		recordDeleter = new RecordDeleterImp(dataUpdater);
		conditions.put("organisation_id", 234);
	}

	@Test
	public void testDataUpdaterInRecordDeleter() {
		assertEquals(recordDeleter.getDataUpdater(), dataUpdater);
	}

	@Test
	public void testDeleteOneRecordOneCondition() {
		recordDeleter.deleteFromTableUsingConditions("organisation", conditions);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordDeleter.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql, "delete from organisation where organisation_id = ?");

		assertEquals(dataUpdaterSpy.values.get(0), 234);
	}

	@Test
	public void testDeleteOneRecordTwoConditions() {
		conditions.put("organisation_name", "someNewOrganisationName");
		recordDeleter.deleteFromTableUsingConditions("organisation", conditions);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordDeleter.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql,
				"delete from organisation where organisation_id = ? and organisation_name = ?");

		assertEquals(dataUpdaterSpy.values.get(0), 234);
		assertEquals(dataUpdaterSpy.values.get(1), "someNewOrganisationName");
	}

}
