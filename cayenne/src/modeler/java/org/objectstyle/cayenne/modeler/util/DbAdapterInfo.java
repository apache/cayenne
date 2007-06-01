/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.modeler.util;

import java.awt.Component;
import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

import org.objectstyle.cayenne.dba.JdbcAdapter;
import org.objectstyle.cayenne.dba.db2.DB2Adapter;
import org.objectstyle.cayenne.dba.firebird.FirebirdAdapter;
import org.objectstyle.cayenne.dba.hsqldb.HSQLDBAdapter;
import org.objectstyle.cayenne.dba.mysql.MySQLAdapter;
import org.objectstyle.cayenne.dba.openbase.OpenBaseAdapter;
import org.objectstyle.cayenne.dba.oracle.OracleAdapter;
import org.objectstyle.cayenne.dba.postgres.PostgresAdapter;
import org.objectstyle.cayenne.dba.sqlserver.SQLServerAdapter;
import org.objectstyle.cayenne.dba.sybase.SybaseAdapter;

/**
 * @author Andrei Adamchik
 */
public final class DbAdapterInfo {

    private static final Map DEFAULT_ADAPTER_LABELS = new TreeMap();
    private static final String[] standardAdapters = new String[] {
            JdbcAdapter.class.getName(), OracleAdapter.class.getName(),
            MySQLAdapter.class.getName(), SybaseAdapter.class.getName(),
            PostgresAdapter.class.getName(), HSQLDBAdapter.class.getName(),
            DB2Adapter.class.getName(), SQLServerAdapter.class.getName(),
            OpenBaseAdapter.class.getName(), FirebirdAdapter.class.getName()
    };

    private static final Map IMMUTABLE_LABELS = Collections
            .unmodifiableMap(DEFAULT_ADAPTER_LABELS);

    static {
        DEFAULT_ADAPTER_LABELS.put(JdbcAdapter.class.getName(), "Generic JDBC Adapter");
        DEFAULT_ADAPTER_LABELS.put(OracleAdapter.class.getName(), "Oracle Adapter");
        DEFAULT_ADAPTER_LABELS.put(MySQLAdapter.class.getName(), "MySQL Adapter");
        DEFAULT_ADAPTER_LABELS.put(SybaseAdapter.class.getName(), "Sybase Adapter");
        DEFAULT_ADAPTER_LABELS.put(PostgresAdapter.class.getName(), "PostgreSQL Adapter");
        DEFAULT_ADAPTER_LABELS.put(HSQLDBAdapter.class.getName(), "HypersonicDB Adapter");
        DEFAULT_ADAPTER_LABELS.put(DB2Adapter.class.getName(), "DB2 Adapter");
        DEFAULT_ADAPTER_LABELS.put(
                SQLServerAdapter.class.getName(),
                "MS SQLServer Adapter");
        DEFAULT_ADAPTER_LABELS.put(OpenBaseAdapter.class.getName(), "OpenBase Adapter");
        DEFAULT_ADAPTER_LABELS.put(FirebirdAdapter.class.getName(), "FireBird Adapter");
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
                object = ((Class) object).getName();
            }

            Object label = adapterLabels.get(object);
            if (label == null) {
                label = object;
            }

            return super.getListCellRendererComponent(list, label, index, arg3, arg4);
        }
    }
}