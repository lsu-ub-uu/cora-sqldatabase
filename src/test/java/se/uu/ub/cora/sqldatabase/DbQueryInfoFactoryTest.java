/*
 * Copyright 2021 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

import org.testng.annotations.Test;

public class DbQueryInfoFactoryTest {

	@Test
	public void testFactorWhenFromAndToIsNull() {
		DbQueryInfoFactory factory = new DbQueryInfoFactoryImp();
		DbQueryInfo queryInfo = factory.factorUsingFromNoAndToNo(null, null);
		assertNull(queryInfo.getFromNo());
		assertNull(queryInfo.getToNo());
	}

	@Test
	public void testFactorWithFromNoAndToNo() {
		DbQueryInfoFactory factory = new DbQueryInfoFactoryImp();
		DbQueryInfo queryInfo = factory.factorUsingFromNoAndToNo(4, 78);
		assertEquals(queryInfo.getFromNo(), Integer.valueOf(4));
		assertEquals(queryInfo.getToNo(), Integer.valueOf(78));
	}
}
