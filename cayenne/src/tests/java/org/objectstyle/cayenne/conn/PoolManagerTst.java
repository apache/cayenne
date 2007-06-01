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
package org.objectstyle.cayenne.conn;

import java.sql.Connection;

import org.objectstyle.cayenne.unit.CayenneTestCase;

public class PoolManagerTst extends CayenneTestCase {

    public void testDataSourceUrl() throws Exception {
        String driverName = getConnectionInfo().getJdbcDriver();
        String url = getConnectionInfo().getDataSourceUrl();

        PoolManager pm = new PoolManager(driverName, url, 0, 3, "", "") {
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(url, pm.getDataSourceUrl());
        assertEquals(driverName, pm.getJdbcDriver());
    }

    public void testPassword() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "b") {
            protected void startMaintenanceThread() {
            }
        };
        assertEquals("b", pm.getPassword());
    }

    public void testUserName() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "a", "") {
            protected void startMaintenanceThread() {
            }
        };
        assertEquals("a", pm.getUserName());
    }

    public void testMinConnections() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "") {
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(0, pm.getMinConnections());
    }

    public void testMaxConnections() throws Exception {
        PoolManager pm = new PoolManager(null, 0, 3, "", "") {
            protected void startMaintenanceThread() {
            }
        };
        assertEquals(3, pm.getMaxConnections());
    }

    public void testPooling() throws Exception {
        DataSourceInfo dsi = getConnectionInfo();
        PoolManager pm =
            new PoolManager(
                dsi.getJdbcDriver(),
                dsi.getDataSourceUrl(),
                2,
                3,
                dsi.getUserName(),
                dsi.getPassword());

        try {
            assertEquals(0, pm.getCurrentlyInUse());
            assertEquals(2, pm.getCurrentlyUnused());

            Connection c1 = pm.getConnection();
            assertEquals(1, pm.getCurrentlyInUse());
            assertEquals(1, pm.getCurrentlyUnused());

            Connection c2 = pm.getConnection();
            assertEquals(2, pm.getCurrentlyInUse());
            assertEquals(0, pm.getCurrentlyUnused());

            c1.close();
            assertEquals(1, pm.getCurrentlyInUse());
            assertEquals(1, pm.getCurrentlyUnused());

            c2.close();
            assertEquals(0, pm.getCurrentlyInUse());
            assertEquals(2, pm.getCurrentlyUnused());
        }
        finally {
            // get rid of local pool
            pm.dispose();
        }

    }
}