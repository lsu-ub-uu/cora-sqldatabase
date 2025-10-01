/*
 * Copyright 2025 Uppsala University Library
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
package se.uu.ub.cora.sqldatabase.sequence;

import se.uu.ub.cora.sqldatabase.SqlConflictException;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.SqlNotFoundException;

/**
 * Interface for a sequence generator that can create, retrieve the next value from, and remove
 * sequences.
 */
public interface Sequence {

	/**
	 * createSequence method Creates a new sequence with the specified name and starting value.
	 *
	 * @param sequenceName
	 *            the name of the sequence to create
	 * @param startValue
	 *            the starting value of the sequence
	 * @thows {@link SqlConflictException} if the sequence already exists
	 * @thows {@link SqlDatabaseException} if there is an error creating the sequence
	 */
	void createSequence(String sequenceName, long startValue);

	/**
	 * getCurrentValueForSequence method retrieves the current value from the sequence.
	 * 
	 * @param sequenceName
	 *            the name of the sequence to get the current value from
	 * 
	 * @thows {@link SqlDatabaseException} if there is an error reading current value
	 * @thows {@link SqlNotFoundException} if the sequence does not exist
	 *
	 * @return the current value in the sequence
	 */
	long getCurrentValueForSequence(String sequenceName);

	/**
	 * getNextValueForSequence method retrieves the next value from the sequence.
	 * 
	 * @param sequenceName
	 *            the name of the sequence to get the next value from
	 * 
	 * @thows {@link SqlDatabaseException} if there is an error reading next value
	 * @thows {@link SqlNotFoundException} if the sequence does not exist
	 *
	 * @return the next value in the sequence
	 */
	long getNextValueForSequence(String sequenceName);

	/**
	 * resetSequenceValue method sets the current value of the sequence to the specified value. The
	 * next call to getNextValueForSequence will return value + 1.
	 * 
	 * @param sequenceName
	 *            the name of the sequence to reset
	 * @param value
	 *            the value to set the sequence to
	 * 
	 * @thows {@link SqlDatabaseException} if there is an error resetting the sequence value
	 * @thows {@link SqlNotFoundException} if the sequence does not exist
	 */
	void resetSequenceValue(String sequenceName, long value);

	/**
	 * removeSequence method removes the sequence with the specified name.
	 *
	 * @param sequenceName
	 *            the name of the sequence to remove
	 * @thows {@link SqlNotFoundException} if the sequence does not exist
	 * @thows {@link SqlDatabaseException} if there is an error removing the sequence
	 */
	void removeSequence(String sequenceName);

}
