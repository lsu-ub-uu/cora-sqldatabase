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

package se.uu.ub.cora.sqldatabase;

import java.util.List;
import java.util.Map;

/**
 * RecordReader reads data from a table in a sql database using conditions on columns.
 */
public interface RecordReader {

	List<Map<String, Object>> readAllFromTable(String tableName);

	List<Map<String, Object>> readFromTableUsingConditions(String tableName,
			Map<String, Object> conditions);

	Map<String, Object> readOneRowFromDbUsingTableAndConditions(String tableName,
			Map<String, Object> conditions);

	/**
	 * TODO: move to {@link DataReader}?? all other methods in this class deals with one table this
	 * is more generic...
	 */
	Map<String, Object> readNextValueFromSequence(String sequenceName);

	List<Map<String, Object>> readAllFromTable(String tableName, DbQueryInfo queryInfo);

	/**
	 * readNumberOfRows returns the numberOfRows in storage that matches the conditions. The number
	 * of rows also depends on limitations set in the DbQueryInfo. The readNumberOfRows SHOULD never
	 * return a larger number than actual result size.
	 * 
	 * @param tableName,
	 *            the table to read from
	 * 
	 * @param conditions,
	 *            the conditions to use in the query. If empty, no conditions are added to query
	 * 
	 * @param DbQueryInfo,
	 *            the dbQueryInfo used to limit the query
	 */
	long readNumberOfRows(String tableName, Map<String, Object> conditions, DbQueryInfo queryInfo);

}
