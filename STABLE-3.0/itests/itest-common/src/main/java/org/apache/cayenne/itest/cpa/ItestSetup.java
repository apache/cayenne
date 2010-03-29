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
package org.apache.cayenne.itest.cpa;

import java.util.Iterator;

import junit.framework.Assert;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.itest.ItestDBUtils;
import org.apache.cayenne.map.DataMap;

public class ItestSetup {

    private static ItestSetup sharedInstance;

    protected ItestDBUtils dbHelper;
    protected DataDomain domain;

    public static void initInstance() {
        sharedInstance = new ItestSetup();
    }

    public static ItestSetup getInstance() {
        Assert.assertNotNull(
                "Null shared instance, call 'initInstance' first",
                sharedInstance);

        return sharedInstance;
    }

    protected ItestSetup() {
        domain = Configuration.getSharedConfiguration().getDomain();

        Iterator it = domain.getDataMaps().iterator();
        while (it.hasNext()) {
            DataMap map = (DataMap) it.next();
            DataNode node = domain.lookupDataNode(map);

            DbGenerator generator = new DbGenerator(node.getAdapter(), map);
            try {
                generator.runGenerator(node.getDataSource());
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Error generating schema for DataMap "
                        + map.getName(), e);
            }

            // only single node is expected...
            dbHelper = new ItestDBUtils(node.getDataSource());
        }
    }

    public DataDomain getDataDomain() {
        return domain;
    }

    public DataContext createDataContext() {
        return domain.createDataContext();
    }

    public ItestDBUtils getDbHelper() {
        return dbHelper;
    }
}
