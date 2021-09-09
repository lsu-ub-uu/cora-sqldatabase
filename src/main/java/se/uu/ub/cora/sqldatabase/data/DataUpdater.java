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
package se.uu.ub.cora.sqldatabase.data;

import java.util.List;

public interface DataUpdater {
	/**
	 * Executes a sql statement as a preparedQuery returning the number of rows affected.
	 * 
	 * @param sql
	 *            A String with the sql statement to execute
	 * @param values
	 *            A List with Objects to use in the prepared statement
	 * @return An int with the number of rows affected by the statement
	 */
	int executeUsingSqlAndValues(String sql, List<Object> values);

}
