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
package se.uu.ub.cora.sqldatabase.table.internal;

import java.util.ArrayList;
import java.util.List;

import se.uu.ub.cora.sqldatabase.table.TableQuery;
import se.uu.ub.cora.testutils.mcr.MethodCallRecorder;

public class TableQuerySpy implements TableQuery {
	MethodCallRecorder MCR = new MethodCallRecorder();

	@Override
	public void addParameter(String name, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addCondition(String name, Object value) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setFromNo(Long fromNo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void setToNo(Long toNo) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOrderByAsc(String column) {
		// TODO Auto-generated method stub

	}

	@Override
	public void addOrderByDesc(String column) {
		// TODO Auto-generated method stub

	}

	@Override
	public String assembleCreateSql() {
		MCR.addCall();
		String sql = "sql for create from spy";
		MCR.addReturned(sql);
		return sql;
	}

	@Override
	public String assembleReadSql() {
		MCR.addCall();
		String sql = "sql for read from spy";
		MCR.addReturned(sql);
		return sql;
	}

	@Override
	public String assembleUpdateSql() {
		MCR.addCall();
		String sql = "sql for update from spy";
		MCR.addReturned(sql);
		return sql;
	}

	@Override
	public String assembleDeleteSql() {
		MCR.addCall();
		String sql = "sql for delete from spy";
		MCR.addReturned(sql);
		return sql;
	}

	@Override
	public List<Object> getQueryValues() {
		MCR.addCall();
		List<Object> out = new ArrayList<>();
		out.add("someValueFromSpy");
		MCR.addReturned(out);
		return out;
	}

	@Override
	public String assembleCountSql() {
		MCR.addCall();
		String sql = "sql for count from spy";
		MCR.addReturned(sql);
		return sql;
	}

}
