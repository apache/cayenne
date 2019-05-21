/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.unit;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.Procedure;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 */
public class SybaseUnitDbAdapter extends UnitDbAdapter {

	public SybaseUnitDbAdapter(DbAdapter adapter) {
		super(adapter);
	}

	public String getIdentifiersStartQuote() {
		return "[";
	}

	public String getIdentifiersEndQuote() {
		return "]";
	}

	@Override
	public boolean supportsStoredProcedures() {
		return true;
	}

	@Override
	public void createdTables(Connection con, DataMap map) throws Exception {
		Procedure proc = map.getProcedure("cayenne_tst_select_proc");
		if (proc != null && proc.getDataMap() == map) {
			executeDDL(con, "sybase", "create-select-sp.sql");
			executeDDL(con, "sybase", "create-update-sp.sql");
			executeDDL(con, "sybase", "create-update-sp2.sql");
			executeDDL(con, "sybase", "create-out-sp.sql");
		}
	}

	@Override
	public void willCreateTables(Connection con, DataMap map) throws Exception {

		// Sybase does not support NULLable BIT columns...
		DbEntity e = map.getDbEntity("PRIMITIVES_TEST");
		if (e != null) {
			e.getAttribute("BOOLEAN_COLUMN").setMandatory(true);
		}
		DbEntity e1 = map.getDbEntity("INHERITANCE_SUB_ENTITY3");
		if (e1 != null) {
			e1.getAttribute("SUBENTITY_BOOL_ATTR").setMandatory(true);
		}
		DbEntity e2 = map.getDbEntity("MT_TABLE_BOOL");
		if (e2 != null) {
			e2.getAttribute("BOOLEAN_COLUMN").setMandatory(true);
		}
		DbEntity e3 = map.getDbEntity("QUALIFIED1");
		if (e3 != null) {
			e3.getAttribute("DELETED").setMandatory(true);
		}

		DbEntity e4 = map.getDbEntity("QUALIFIED2");
		if (e4 != null) {
			e4.getAttribute("DELETED").setMandatory(true);
		}

		DbEntity e5 = map.getDbEntity("Painting");
		if (e5 != null) {
			if (e5.getAttribute("NEWCOL2") != null) {
				e5.getAttribute("DELETED").setMandatory(true);
			}
		}

		DbEntity e6 = map.getDbEntity("SOFT_TEST");
		if (e6 != null) {
			e6.getAttribute("DELETED").setMandatory(true);
		}

	}

	@Override
	public void willDropTables(Connection con, DataMap map, Collection tablesToDrop) throws Exception {

		Iterator it = tablesToDrop.iterator();
		while (it.hasNext()) {
			dropConstraints(con, (String) it.next());
		}

		dropProcedures(con, map);
	}

	protected void dropProcedures(Connection con, DataMap map) throws Exception {
		Procedure proc = map.getProcedure("cayenne_tst_select_proc");
		if (proc != null && proc.getDataMap() == map) {
			executeDDL(con, "sybase", "drop-select-sp.sql");
			executeDDL(con, "sybase", "drop-update-sp.sql");
			executeDDL(con, "sybase", "drop-update-sp2.sql");
			executeDDL(con, "sybase", "drop-out-sp.sql");
		}
	}

	protected void dropConstraints(Connection con, String tableName) throws Exception {
		List<String> names = new ArrayList<>(3);

		try (Statement select = con.createStatement();) {

			try (ResultSet rs = select.executeQuery("SELECT t0.name "
					+ "FROM sysobjects t0, sysconstraints t1, sysobjects t2 "
					+ "WHERE t0.id = t1.constrid and t1.tableid = t2.id and t2.name = '" + tableName + "'");) {

				while (rs.next()) {
					names.add(rs.getString("name"));
				}
			}
		}

		for (String name : names) {
			executeDDL(con, "alter table " + tableName + " drop constraint " + name);
		}
	}

	@Override
	public boolean supportsLobs() {
		return true;
	}

	@Override
	public boolean handlesNullVsEmptyLOBs() {
		// TODO Sybase handling of this must be fixed
		return false;
	}

	@Override
	public boolean supportsNullBoolean() {
		return false;
	}
}
