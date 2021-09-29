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

package se.uu.ub.cora.sqldatabase.internal;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import se.uu.ub.cora.sqldatabase.DatabaseValues;
import se.uu.ub.cora.sqldatabase.Row;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

public class RowImp implements Row {
	private Map<String, Object> columnValues = new HashMap<>();

	public void addColumnWithValue(String columnName, Object object) {
		if (object == null) {
			columnValues.put(columnName, DatabaseValues.NULL);
		} else {
			columnValues.put(columnName, object);
		}
	}

	@Override
	public Object getValueByColumn(String columnName) {
		if (columnValues.containsKey(columnName)) {
			return columnValues.get(columnName);
		}
		throw SqlDatabaseException.withMessage("Column: " + columnName + ", does not exist");
	}

	@Override
	public Set<String> columnSet() {
		return columnValues.keySet();
	}

	@Override
	public boolean hasColumn(String columnName) {
		return columnValues.containsKey(columnName);
	}

	@Override
	public boolean hasColumnWithNonEmptyValue(String columnName) {
		if (hasColumn(columnName)) {
			return columnHasValue(columnName);
		}
		return false;
	}

	private boolean columnHasValue(String columnName) {
		Object value = columnValues.get(columnName);
		return (DatabaseValues.NULL != value && !isStringWithoutValue(value));
	}

	private boolean isStringWithoutValue(Object value) {
		return (value instanceof String stringValue && stringValue.isBlank());
	}

}
