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

import se.uu.ub.cora.initialize.SettingsProvider;
import se.uu.ub.cora.recordlock.RecordLock;
import se.uu.ub.cora.recordlock.RecordLockException;
import se.uu.ub.cora.recordlock.RecordLockInstanceProvider;
import se.uu.ub.cora.sqldatabase.SqlDatabaseFactory;
import se.uu.ub.cora.sqldatabase.SqlDatabaseFactoryImp;

/**
 * RecordLockInstanceProviderImp provides database backed {@link RecordLock}s. It is found by
 * RecordLockProvider in the recordlock module through the java module system.
 * <p>
 * The database is found using the lookup name in the setting <em>coraDatabaseLookupName</em>.
 */
public class RecordLockInstanceProviderImp implements RecordLockInstanceProvider {
	private static final String DATABASE_LOOKUP_NAME = "coraDatabaseLookupName";

	@Override
	public int getOrderToSelectImplementionsBy() {
		return 0;
	}

	@Override
	public RecordLock getRecordLock() {
		try {
			return RecordLockImp.usingSqlDatabaseFactory(createSqlDatabaseFactory());
		} catch (Exception e) {
			throw RecordLockException.withMessageAndException("Getting RecordLock failed", e);
		}
	}

	private SqlDatabaseFactory createSqlDatabaseFactory() {
		String lookupName = SettingsProvider.getSetting(DATABASE_LOOKUP_NAME);
		return SqlDatabaseFactoryImp.usingLookupNameFromContext(lookupName);
	}
}
