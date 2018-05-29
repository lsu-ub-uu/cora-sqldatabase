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
package se.uu.ub.cora.connection;

import java.sql.Connection;
import java.sql.DriverManager;

import se.uu.ub.cora.sqldatabase.SqlStorageException;

public final class ParameterConnectionProviderImp implements SqlConnectionProvider {
	private String url;
	private String user;
	private String password;

	public static ParameterConnectionProviderImp usingUriAndUserAndPassword(String url, String user,
			String password) {
		return new ParameterConnectionProviderImp(url, user, password);
	}

	private ParameterConnectionProviderImp(String url, String user, String password) {
		this.url = url;
		this.user = user;
		this.password = password;
	}

	@Override
	public Connection getConnection() {
		try {
			return DriverManager.getConnection(url, user, password);
		} catch (Exception e) {
			throw SqlStorageException.withMessageAndException("Error getting connection", e);
		}
	}

}
