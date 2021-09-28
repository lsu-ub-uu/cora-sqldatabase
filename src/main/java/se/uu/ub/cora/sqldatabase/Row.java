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
	 * getValueByColumn returns the value for the requested column.
	 * <p>
	 * Implemementations MUST return {@link DatabaseValues#NULL} for null values found in the
	 * database.
	 * 
	 * @param columnName
	 *            A String with the name of the column to return the value for
	 * @return An Object with the value for the column
	 */
	Object getValueByColumn(String columnName);

	/**
	 * columnSet returns a Set with the column names that this Row has
	 * 
	 * @return A Set of Strings with the names that exist in this Row
	 */
	Set<String> columnSet();

	/**
	 * hasColumn returns true if this Row has a column with the requested column name
	 * 
	 * @param columnName
	 *            A String with the name of the column to check for in this Row
	 * @return A boolean true if the requested column exists in this Row
	 */
	boolean hasColumn(String columnName);

}
