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

public class RecordUpdaterTest {

	private DataUpdater dataUpdater;
	private RecordUpdaterImp recordUpdater;
	private Map<String, Object> values = new HashMap<>();
	private Map<String, Object> conditions = new HashMap<>();

	@BeforeMethod
	public void setUp() {
		dataUpdater = new DataUpdaterSpy();
		recordUpdater = new RecordUpdaterImp(dataUpdater);
		values.put("organisation_name", "someNewOrganisationName");
		conditions.put("organisation_id", 123);
	}

	@Test
	public void testDataUpdaterInRecordUpdater() {
		assertEquals(recordUpdater.getDataUpdater(), dataUpdater);
	}

	@Test
	public void testUpdateOneRecordOneColumnOneCondition() {
		recordUpdater.updateTableUsingNameAndColumnsWithValuesAndConditions("organisation", values,
				conditions);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordUpdater.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql,
				"update organisation set organisation_name = ? where organisation_id = ?");

		assertEquals(dataUpdaterSpy.values.get(0), "someNewOrganisationName");
		assertEquals(dataUpdaterSpy.values.get(1), 123);
	}

	@Test
	public void testUpdateOneRecordTwoColumnsOneCondition() {
		values.put("organisation_code", "someNewOrgCode");
		recordUpdater.updateTableUsingNameAndColumnsWithValuesAndConditions("organisation", values,
				conditions);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordUpdater.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql,
				"update organisation set organisation_code = ?, organisation_name = ? where organisation_id = ?");

		assertEquals(dataUpdaterSpy.values.get(0), "someNewOrgCode");
		assertEquals(dataUpdaterSpy.values.get(1), "someNewOrganisationName");
		assertEquals(dataUpdaterSpy.values.get(2), 123);
	}

	@Test
	public void testUpdateOneRecordTwoColumnsTwoConditions() {
		values.put("organisation_code", "someNewOrgCode");
		conditions.put("country_code", "swe");
		recordUpdater.updateTableUsingNameAndColumnsWithValuesAndConditions("organisation", values,
				conditions);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordUpdater.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql,
				"update organisation set organisation_code = ?, organisation_name = ? where organisation_id = ? and country_code = ?");

		assertEquals(dataUpdaterSpy.values.get(0), "someNewOrgCode");
		assertEquals(dataUpdaterSpy.values.get(1), "someNewOrganisationName");
		assertEquals(dataUpdaterSpy.values.get(2), 123);
		assertEquals(dataUpdaterSpy.values.get(3), "swe");
	}

}
