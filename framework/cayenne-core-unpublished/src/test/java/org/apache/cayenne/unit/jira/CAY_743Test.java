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
package org.apache.cayenne.unit.jira;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.configuration.server.ServerModule;
import org.apache.cayenne.di.DIBootstrap;
import org.apache.cayenne.di.Injector;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;

public class CAY_743Test extends TestCase {

    public void testLoad2MapsWithCrossMapInheritance() throws Exception {

        Injector injector = DIBootstrap.createInjector(new ServerModule(
                "cay743/cayenne-domain.xml"));

        try {
            DataDomain domain = injector.getInstance(DataDomain.class);
            assertEquals(2, domain.getDataMaps().size());

            DataMap m1 = domain.getDataMap("map1");
            DataMap m2 = domain.getDataMap("map2");

            ObjEntity oe11 = m1.getObjEntity("Entity11");
            ObjEntity oe12 = m1.getObjEntity("Entity12");

            ObjEntity oe21 = m2.getObjEntity("Entity21");
            ObjEntity oe22 = m2.getObjEntity("Entity22");

            // this causes StackOverflow per CAY-743
            ObjEntity oe21Super = oe21.getSuperEntity();
            ObjEntity oe12Super = oe12.getSuperEntity();

            assertSame(oe12Super, oe22);
            assertSame(oe21Super, oe11);
        }
        finally {
            injector.shutdown();
        }
    }
}
