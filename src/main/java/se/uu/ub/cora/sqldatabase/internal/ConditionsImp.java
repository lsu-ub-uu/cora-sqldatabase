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

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import se.uu.ub.cora.sqldatabase.Conditions;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

public class ConditionsImp implements Conditions {
	private LinkedHashMap<String, Object> conditions = new LinkedHashMap<>();

	@Override
	public void add(String conditionName, Object conditionValue) {
		conditions.put(conditionName, conditionValue);
	}

	@Override
	public Object getValue(String conditionName) {
		if (conditions.containsKey(conditionName)) {
			return conditions.get(conditionName);
		}
		throw SqlDatabaseException.withMessage("Condition: " + conditionName + ", does not exist");
	}

	@Override
	public List<String> getNames() {
		return new ArrayList<>(conditions.keySet());
	}

	@Override
	public List<Object> getValues() {
		return new ArrayList<>(conditions.values());
	}

	@Override
	public boolean hasConditions() {
		return conditions.size() > 0;
	}

}
