/*
 * Copyright 2018 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.connection;

import static org.testng.Assert.assertEquals;

import java.sql.Connection;
import java.sql.DriverManager;

import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class ParameterConnectionProviderTest {

	private DriverSpy driver;

	@BeforeMethod
	public void beforeMethod() throws Exception {
		driver = new DriverSpy();
		DriverManager.registerDriver(driver);
	}

	@AfterMethod
	public void afterMethod() throws Exception {
		DriverManager.deregisterDriver(driver);
	}

	@Test
	public void testInit() throws Exception {
		String url = "jdbc:NOTpostgresqlNOT://alvin-cora-docker-postgresql:5432/alvin";
		String user = "someUserId";
		String password = "somePassword";
		ParameterConnectionProviderImp connectionProvider = ParameterConnectionProviderImp
				.usingUriAndUserAndPassword(url, user, password);
		Connection connection = connectionProvider.getConnection();
		assertEquals(connection, driver.connectionSpy);
		assertEquals(driver.url, url);
		assertEquals(driver.info.getProperty("user"), user);
		assertEquals(driver.info.getProperty("password"), password);
	}

	@Test
	public void testError() throws Exception {
		String url = "jdbc:NOTpostgresqlNOT://alvin-cora-docker-postgresql:5432/alvin";
		String user = "INVALIDuserId";
		String password = "somePassword";
		ParameterConnectionProviderImp connectionProvider = ParameterConnectionProviderImp
				.usingUriAndUserAndPassword(url, user, password);
		try {
			connectionProvider.getConnection();
		} catch (Exception e) {
			assertEquals(e.getMessage(), "Error getting connection");
			assertEquals(e.getCause().getMessage(), "throwing error from DriverSpy");
		}

	}
}
