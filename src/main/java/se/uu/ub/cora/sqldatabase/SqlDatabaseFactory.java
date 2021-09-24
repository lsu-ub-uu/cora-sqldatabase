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

import se.uu.ub.cora.sqldatabase.table.TableFacade;

/**
 * SqlDatabaseFactory is a factory interface that provides instances of classes in the sqlDatabase
 * module.
 * <p>
 * To be able to fullfill this interface must implementing factories be supplied with connection
 * details for the database, how this is done is up to the implementing classes to decide.
 * <p>
 * Implementations of SqlDatabaseFactory MUST be threadsafe.
 */
public interface SqlDatabaseFactory {
	/**
	 * factorDatabaseFacade creates and returns a new instance of DatabaseFacade. The returned
	 * DatabaseFacade should by the implementing factory be set up with database connection details
	 * so that it can be used to interact with the database without any further configuration.
	 * <p>
	 * <em>Note, DatabaseFacade uses the {@link AutoCloseable} interface so it is adviced to wrapp
	 * this call in an try-with-resources block.</em>
	 * 
	 * @return A DatabaseFacade set up with connection details for the database
	 */
	DatabaseFacade factorDatabaseFacade();

	/**
	 * factorTableFacade creates and returns a new instance of TableFacade. The returned TableFacade
	 * should by the implementing factory be set up with database connection details so that it can
	 * be used to interact with the database without any further configuration.
	 * <p>
	 * <em>Note, TableFacade uses the {@link AutoCloseable} interface so it is adviced to wrapp this
	 * call in an try-with-resources block.</em>
	 * 
	 * @return A TableFacade set up with connection details for the database
	 */
	TableFacade factorTableFacade();

}
