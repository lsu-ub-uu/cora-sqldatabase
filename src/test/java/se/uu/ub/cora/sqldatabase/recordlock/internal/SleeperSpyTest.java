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

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertSame;
import static org.testng.Assert.fail;

import org.testng.annotations.Test;

public class SleeperSpyTest {

	@Test
	public void testSleepMillisecondsIsRecorded() {
		SleeperSpy sleeper = new SleeperSpy();

		sleeper.sleepMilliseconds(50);

		sleeper.MCR.assertParameters("sleepMilliseconds", 0, 50L);
	}

	@Test
	public void testSleepMillisecondsCanThrowException() {
		SleeperSpy sleeper = new SleeperSpy();
		RuntimeException error = new RuntimeException("someError");
		sleeper.MRV.setAlwaysThrowException("sleepMilliseconds", error);

		try {
			sleeper.sleepMilliseconds(50);
			fail();
		} catch (Exception e) {
			assertSame(e, error);
		}
	}

	@Test
	public void testTimeSourceSpyDefaultAndConfiguredValues() {
		TimeSourceSpy timeSource = new TimeSourceSpy();

		assertEquals(timeSource.currentTimeMillis(), 0L);
		timeSource.MRV.setDefaultReturnValuesSupplier("currentTimeMillis", () -> 7L);
		assertEquals(timeSource.currentTimeMillis(), 7L);
		timeSource.MCR.assertNumberOfCallsToMethod("currentTimeMillis", 2);
	}
}
