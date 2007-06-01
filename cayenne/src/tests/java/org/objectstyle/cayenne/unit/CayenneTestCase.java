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
package org.objectstyle.cayenne.unit;

import java.sql.Connection;
import java.util.Iterator;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataDomain;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.conf.Configuration;
import org.objectstyle.cayenne.conf.DefaultConfiguration;
import org.objectstyle.cayenne.conn.DataSourceInfo;
import org.objectstyle.cayenne.event.EventManager;
import org.objectstyle.cayenne.map.DataMap;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.map.ObjEntity;
import org.objectstyle.cayenne.unit.util.SQLTemplateCustomizer;

/**
 * Superclass of Cayenne test cases. Provides access to shared
 * connection resources.
 * 
 * @author Andrei Adamchik
 */
public abstract class CayenneTestCase extends BasicTestCase {
    public static final String TEST_ACCESS_STACK = "TestStack";

    static {
        // create dummy shared config
        Configuration config = new DefaultConfiguration() {
            public void initialize() {
            }
        };

        Configuration.initializeSharedConfiguration(config);

        // make sure CayenneTestResources is loaded
        CayenneTestResources.class.getName();
    }

    protected AccessStack accessStack;

    public CayenneTestCase() {
        this.accessStack = buildAccessStack();
    }

    protected AccessStack buildAccessStack() {
        return CayenneTestResources.getResources().getAccessStack(TEST_ACCESS_STACK);
    }

    protected AccessStackAdapter getAccessStackAdapter() {
        return accessStack.getAdapter(getNode());
    }

    protected DataSourceInfo getConnectionInfo() throws Exception {
        return CayenneTestResources.getResources().getConnectionInfo();
    }

    protected Connection getConnection() throws Exception {
        return getNode().getDataSource().getConnection();
    }

    protected DataDomain getDomain() {
        return accessStack.getDataDomain();
    }

    protected SQLTemplateCustomizer getSQLTemplateBuilder() {
        SQLTemplateCustomizer customizer =
            CayenneTestResources.getResources().getSQLTemplateCustomizer();

        // make sure adapter is correct
        customizer.setAdapter(getAccessStackAdapter().getAdapter());
        return customizer;
    }

    /**
     * Creates test data via a mechanism preconfigured in the access stack.
     * Default mechanism is loading test data DML from XML file. 
     */
    protected void createTestData(String testName) throws Exception {
        accessStack.createTestData(this.getClass(), testName);
    }

    protected DataNode getNode() {
        return (DataNode) getDomain().getDataNodes().iterator().next();
    }

    protected DataContext createDataContext() {
        return createDataContextWithSharedCache();
    }

    /**
     * Creates a DataContext that uses shared snapshot cache and is based on default test domain.
     */
    protected DataContext createDataContextWithSharedCache() {
        // remove listeners for snapshot events
        EventManager.getDefaultManager().removeAllListeners(
            getDomain().getSharedSnapshotCache().getSnapshotEventSubject());

        // clear cache...
        getDomain().getSharedSnapshotCache().clear();
        DataContext context = getDomain().createDataContext(true);

        assertSame(
            getDomain().getSharedSnapshotCache(),
            context.getObjectStore().getDataRowCache());

        return context;
    }

    /**
     * Creates a DataContext that uses local snapshot cache and is based on default test domain.
     */
    protected DataContext createDataContextWithLocalCache() {
        DataContext context = getDomain().createDataContext(false);

        assertNotSame(
            getDomain().getSharedSnapshotCache(),
            context.getObjectStore().getDataRowCache());

        return context;
    }

    /**
     * Returns AccessStack.
     */
    protected AccessStack getAccessStack() {
        return accessStack;
    }

    protected void deleteTestData() throws Exception {
        accessStack.deleteTestData();
    }
    
    protected DbEntity getDbEntity(String dbEntityName) {
        // retrieve DbEntity the hard way, bypassing the resolver...
        Iterator it = getDomain().getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            Iterator dbEntities = map.getDbEntities().iterator();
            while (dbEntities.hasNext()) {
                DbEntity e = (DbEntity) dbEntities.next();
                if (dbEntityName.equals(e.getName())) {
                    return e;
                }
            }
        }

        throw new CayenneRuntimeException("No DbEntity found: " + dbEntityName);
    }
    
    protected ObjEntity getObjEntity(String objEntityName) {
        // retrieve ObjEntity the hard way, bypassing the resolver...
        Iterator it = getDomain().getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            Iterator objEntities = map.getObjEntities().iterator();
            while (objEntities.hasNext()) {
                ObjEntity e = (ObjEntity) objEntities.next();
                if (objEntityName.equals(e.getName())) {
                    return e;
                }
            }
        }

        throw new CayenneRuntimeException("No ObjEntity found: " + objEntityName);
    }
}
