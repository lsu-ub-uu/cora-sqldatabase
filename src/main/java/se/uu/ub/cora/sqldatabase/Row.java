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

import java.util.Set;

/**
 * The Row interface represents a database row, giving access to reading values from a database
 * query result.
 */
public interface Row {

	/**
	 * 
	 * @param columnName
	 * @return
	 */
	Object getValueByColumn(String columnName);

	/**
	 * 
	 * @return
	 */
	Set<String> columnSet();

	/**
	 * 
	 * @param columnName
	 * @return
	 */
	boolean hasColumn(String columnName);

}
