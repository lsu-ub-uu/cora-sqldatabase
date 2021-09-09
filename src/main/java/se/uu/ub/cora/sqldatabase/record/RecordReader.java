/*
 * Copyright 2018, 2021 Uppsala University Library
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

package se.uu.ub.cora.sqldatabase.record;

import java.util.List;
import java.util.Map;

import se.uu.ub.cora.sqldatabase.DbQueryInfo;
import se.uu.ub.cora.sqldatabase.SqlDatabaseException;
import se.uu.ub.cora.sqldatabase.data.DataReader;

/**
 * RecordReader reads data from sql database without the need to write sql statements.
 * <p>
 * If you need to use more generic sql statements use {@link DataReader} instead.
 */
public interface RecordReader {

	List<Map<String, Object>> readAllFromTable(String tableName);

	List<Map<String, Object>> readAllFromTable(String tableName, DbQueryInfo queryInfo);

	/**
	 * readOneRowFromDbUsingTableAndConditions reads one row from the database using a tablename and
	 * conditions.
	 * <p>
	 * If no row or more than one row is found matching the conditions MUST a
	 * {@link SqlDatabaseException} be thrown, indicating that the requested single row can not be
	 * realibly read.
	 * 
	 * @param tableName
	 *            the table to read from
	 * @param conditions
	 *            A Map<String, Object> with the columnName as key and requested value as value to
	 *            use in the query.
	 * @return A Map<String, Object> with the columnNames from the result as key and the
	 *         corresponding values
	 */
	Map<String, Object> readOneRowFromDbUsingTableAndConditions(String tableName,
			Map<String, Object> conditions);

	List<Map<String, Object>> readFromTableUsingConditions(String tableName,
			Map<String, Object> conditions);

	// List<Map<String, Object>> readFromTableUsingConditions(String tableName, Map<String, Object>
	// conditions,DbQueryInfo queryInfo);

	/**
	 * readNumberOfRows returns the numberOfRows in storage that matches the conditions. The number
	 * of rows also depends on limitations set in the DbQueryInfo. The readNumberOfRows SHOULD never
	 * return a larger number than actual result size.
	 * 
	 * @param tableName,
	 *            the table to read from
	 * 
	 * @param conditions,
	 *            A Map<String, Object> with the columnName as key and requested value as value to
	 *            use in the query. If the conditions map is empty, are no parameters added to the
	 *            query.
	 * 
	 * @param DbQueryInfo,
	 *            the dbQueryInfo used to limit the query
	 */
	long readNumberOfRows(String tableName, Map<String, Object> conditions, DbQueryInfo queryInfo);

	Map<String, Object> readNextValueFromSequence(String sequenceName);

}