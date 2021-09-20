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

import java.util.List;

/**
 * Conditions represent the condition part of a sql query. Each condition has two sub parts, name
 * and value. Conditions are used as the WHERE clause in a sql query.
 */
public interface Conditions {

	/**
	 * The add method adds a condition with name and value to this conditions.
	 * <p>
	 * If a condition is to use null, MUST the value of the added condition be {@link DatabaseNull}
	 * 
	 * @param name
	 *            A String with the name to use in the sql query
	 * 
	 * @param value
	 *            A String with the value to use in the sql query
	 *
	 */
	void add(String name, Object value);

	/**
	 * getValue method is used to get the value of a condition that matches the condition name.
	 * 
	 * @param name
	 *            A String with the name of the condition.
	 * @return An Object with the value of the condition.
	 */
	Object getValue(String name);

	/**
	 * getNames returns a list of names from each condition that exist in this class. The list of
	 * names are returned in the same order as the conditions were added.
	 * 
	 * @return A List of Strings with the names for the conditions
	 */
	List<String> getNames();

	/**
	 * getValues returns a list of values from each condition that exist in this class. The list of
	 * values are returned in the same order as the conditions were added.
	 * 
	 * @return A List of Objects with the values for the conditions
	 */
	List<Object> getValues();

	/**
	 * hasConditions returns true if at least one condition have been added, else false is returned
	 * 
	 * @return A boolean, true if there are conditions else false
	 */
	boolean hasConditions();

}
