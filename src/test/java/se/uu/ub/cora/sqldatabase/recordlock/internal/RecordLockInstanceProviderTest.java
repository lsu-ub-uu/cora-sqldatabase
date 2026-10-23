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
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Provides;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import se.uu.ub.cora.initialize.InitializationException;
import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.logger.LoggerProvider;
import se.uu.ub.cora.recordlock.RecordLockException;
import se.uu.ub.cora.recordlock.RecordLockInstanceProvider;
import se.uu.ub.cora.sqldatabase.SqlDatabaseFactoryImp;
import se.uu.ub.cora.testspies.logger.LoggerFactorySpy;

public class RecordLockInstanceProviderTest {
	private static final String SERVICE_FILE = "META-INF/services/"
			+ RecordLockInstanceProvider.class.getName();
	private RecordLockInstanceProvider provider;

	@BeforeMethod
	public void beforeMethod() {
		LoggerProvider.setLoggerFactory(new LoggerFactorySpy());
		SettingsProvider.setSettings(Map.of("coraDatabaseLookupName", "someLookupName"));
		provider = new RecordLockInstanceProviderImp();
	}

	@Test
	public void testGetOrderToSelectImplementionsBy() {
		assertEquals(provider.getOrderToSelectImplementionsBy(), 0);
	}

	@Test
	public void testGetRecordLockIsWiredWithSqlDatabaseFactoryUsingLookupNameFromSettings() {
		RecordLockImp recordLock = (RecordLockImp) provider.getRecordLock();

		SqlDatabaseFactoryImp sqlDatabaseFactory = (SqlDatabaseFactoryImp) recordLock
				.onlyForTestGetSqlDatabaseFactory();
		assertEquals(sqlDatabaseFactory.onlyForTestGetLookupName(), "someLookupName");
		assertTrue(recordLock.onlyForTestGetSleeper() instanceof ThreadSleeper);
		assertTrue(recordLock.onlyForTestGetTimeSource() instanceof SystemTimeSource);
	}

	@Test
	public void testGetRecordLockReturnsNewInstanceForEachCall() {
		assertNotSame(provider.getRecordLock(), provider.getRecordLock());
	}

	@Test
	public void testCreatingProviderDoesNotReadSettings() {
		SettingsProvider.setSettings(Collections.emptyMap());

		new RecordLockInstanceProviderImp();
	}

	@Test
	public void testGetRecordLockMissingSettingIsTranslated() {
		SettingsProvider.setSettings(Collections.emptyMap());

		try {
			provider.getRecordLock();
			fail("RecordLockException should have been thrown");
		} catch (Exception e) {
			assertTrue(e instanceof RecordLockException);
			assertEquals(e.getMessage(), "Getting RecordLock failed");
			assertTrue(e.getCause() instanceof InitializationException);
		}
	}

	@Test
	public void testProviderIsRegisteredInServiceFile() throws Exception {
		List<String> registered = readServiceFile();

		assertEquals(registered, List.of(RecordLockInstanceProviderImp.class.getName()));
	}

	private List<String> readServiceFile() throws Exception {
		try (InputStream stream = RecordLockInstanceProviderImp.class.getClassLoader()
				.getResourceAsStream(SERVICE_FILE);
				BufferedReader reader = new BufferedReader(
						new InputStreamReader(stream, StandardCharsets.UTF_8))) {
			return reader.lines().map(String::trim).filter(line -> !line.isEmpty()).toList();
		}
	}

	@Test
	public void testProviderIsRegisteredInModuleDescriptorWhenRunningAsModule() {
		Module module = RecordLockInstanceProviderImp.class.getModule();
		if (module.isNamed()) {
			ModuleDescriptor descriptor = module.getDescriptor();
			Provides provides = descriptor.provides().stream()
					.filter(p -> p.service().equals(RecordLockInstanceProvider.class.getName()))
					.findFirst().orElseThrow();
			assertEquals(provides.providers(),
					List.of(RecordLockInstanceProviderImp.class.getName()));
		}
	}
}
