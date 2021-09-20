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
package se.uu.ub.cora.sqldatabase.table;

public interface DbQueryInfoFactory {

	/**
	 * factorUsingFromAndToNo factores a DbQueryInfo, using fromNo and toNo. The method SHOULD
	 * handle the possibilty that either fromNo or toNo is null, and return an instance of
	 * DbQueryInfo regardless.
	 * 
	 * @param Integer
	 *            fromNo, the from number to set in the DbQueryInfo
	 * 
	 * @param Integer
	 *            toNo, the to number to set in the DbQueryInfo
	 */
	DbQueryInfo factorUsingFromNoAndToNo(Integer fromNo, Integer toNo);

}
