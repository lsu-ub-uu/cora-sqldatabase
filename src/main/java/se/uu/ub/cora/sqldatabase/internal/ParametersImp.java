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

import se.uu.ub.cora.sqldatabase.Parameters;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;

public class ParametersImp implements Parameters {
	private LinkedHashMap<String, Object> parameters = new LinkedHashMap<>();

	@Override
	public void add(String name, Object value) {
		parameters.put(name, value);
	}

	@Override
	public Object getValue(String name) {
		if (parameters.containsKey(name)) {
			return parameters.get(name);
		}
		throw SqlDatabaseException.withMessage("Parameter: " + name + ", does not exist");
	}

	@Override
	public List<String> getNames() {
		return new ArrayList<>(parameters.keySet());
	}

	@Override
	public List<Object> getValues() {
		return new ArrayList<>(parameters.values());
	}

}
