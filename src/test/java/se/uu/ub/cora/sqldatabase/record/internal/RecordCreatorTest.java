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
import se.uu.ub.cora.sqldatabase.record.RecordCreator;

public class RecordCreatorTest {

	private DataUpdater dataUpdater;
	private RecordCreator recordCreator;
	private Map<String, Object> values = new HashMap<>();

	@BeforeMethod
	public void setUp() {
		dataUpdater = new DataUpdaterSpy();
		recordCreator = new RecordCreatorImp(dataUpdater);
		values.put("organisation_id", 234);
		values.put("organisation_name", "someNewOrganisationName");
	}

	@Test
	public void testDataUpdaterInRecordCreator() {
		assertEquals(recordCreator.getDataUpdater(), dataUpdater);
	}

	@Test
	public void testInsertOneRecord() {
		recordCreator.insertIntoTableUsingNameAndColumnsWithValues("organisation", values);
		DataUpdaterSpy dataUpdaterSpy = (DataUpdaterSpy) recordCreator.getDataUpdater();
		assertEquals(dataUpdaterSpy.sql,
				"insert into organisation(organisation_id, organisation_name) values(?, ?)");

		assertEquals(dataUpdaterSpy.values.get(0), 234);
		assertEquals(dataUpdaterSpy.values.get(1), "someNewOrganisationName");
	}

}
