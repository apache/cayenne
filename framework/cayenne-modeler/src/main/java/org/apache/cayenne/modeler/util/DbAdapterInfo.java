/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.modeler.util;

import java.awt.Component;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.dba.db2.DB2Adapter;
import org.apache.cayenne.dba.derby.DerbyAdapter;
import org.apache.cayenne.dba.frontbase.FrontBaseAdapter;
import org.apache.cayenne.dba.h2.H2Adapter;
import org.apache.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.apache.cayenne.dba.ingres.IngresAdapter;
import org.apache.cayenne.dba.mysql.MySQLAdapter;
import org.apache.cayenne.dba.openbase.OpenBaseAdapter;
import org.apache.cayenne.dba.oracle.OracleAdapter;
import org.apache.cayenne.dba.postgres.PostgresAdapter;
import org.apache.cayenne.dba.sqlite.SQLiteAdapter;
import org.apache.cayenne.dba.sqlserver.SQLServerAdapter;
import org.apache.cayenne.dba.sybase.SybaseAdapter;

/**
 */
public final class DbAdapterInfo {

    private static final Map<String, String> DEFAULT_ADAPTER_LABELS = new TreeMap<String, String>();
    private static final String[] standardAdapters = new String[] {
            JdbcAdapter.class.getName(), MySQLAdapter.class.getName(),
            OracleAdapter.class.getName(), SybaseAdapter.class.getName(),
            PostgresAdapter.class.getName(), H2Adapter.class.getName(),
            HSQLDBAdapter.class.getName(), DB2Adapter.class.getName(),
            SQLServerAdapter.class.getName(), FrontBaseAdapter.class.getName(),
            OpenBaseAdapter.class.getName(), DerbyAdapter.class.getName(),
            IngresAdapter.class.getName(), SQLiteAdapter.class.getName()
    };

    private static final Map<String, String> IMMUTABLE_LABELS = Collections
            .unmodifiableMap(DEFAULT_ADAPTER_LABELS);

    static {
        DEFAULT_ADAPTER_LABELS.put(JdbcAdapter.class.getName(), "Generic JDBC Adapter");
        DEFAULT_ADAPTER_LABELS.put(OracleAdapter.class.getName(), "Oracle Adapter");
        DEFAULT_ADAPTER_LABELS.put(MySQLAdapter.class.getName(), "MySQL Adapter");
        DEFAULT_ADAPTER_LABELS.put(SybaseAdapter.class.getName(), "Sybase Adapter");
        DEFAULT_ADAPTER_LABELS.put(PostgresAdapter.class.getName(), "PostgreSQL Adapter");
        DEFAULT_ADAPTER_LABELS.put(HSQLDBAdapter.class.getName(), "HypersonicDB Adapter");
        DEFAULT_ADAPTER_LABELS.put(H2Adapter.class.getName(), " H2 Database Adapter");
        DEFAULT_ADAPTER_LABELS.put(DB2Adapter.class.getName(), "DB2 Adapter");
        DEFAULT_ADAPTER_LABELS.put(
                SQLServerAdapter.class.getName(),
                "MS SQLServer Adapter");
        DEFAULT_ADAPTER_LABELS.put(FrontBaseAdapter.class.getName(), "FrontBase Adapter");
        DEFAULT_ADAPTER_LABELS.put(OpenBaseAdapter.class.getName(), "OpenBase Adapter");
        DEFAULT_ADAPTER_LABELS.put(DerbyAdapter.class.getName(), "Derby Adapter");
        DEFAULT_ADAPTER_LABELS.put(IngresAdapter.class.getName(), "Ingres Adapter");
        DEFAULT_ADAPTER_LABELS.put(SQLiteAdapter.class.getName(), "SQLite Adapter");
    }

    public static Map getStandardAdapterLabels() {
        return IMMUTABLE_LABELS;
    }

    public static ListCellRenderer getListRenderer() {
        return new DbAdapterListRenderer(DEFAULT_ADAPTER_LABELS);
    }

    public static Object[] getStandardAdapters() {
        return standardAdapters;
    }

    static final class DbAdapterListRenderer extends DefaultListCellRenderer {

        Map adapterLabels;

        DbAdapterListRenderer(Map adapterLabels) {
            this.adapterLabels = (adapterLabels != null)
                    ? adapterLabels
                    : Collections.EMPTY_MAP;
        }

        public Component getListCellRendererComponent(
                JList list,
                Object object,
                int index,
                boolean arg3,
                boolean arg4) {

            if (object instanceof Class) {
                object = ((Class<?>) object).getName();
            }

            Object label = adapterLabels.get(object);
            if (label == null) {
                label = object;
            }

            return super.getListCellRendererComponent(list, label, index, arg3, arg4);
        }
    }
}
