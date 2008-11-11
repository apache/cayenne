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

package org.apache.cayenne.project;

import java.io.File;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataMapFileTest extends CayenneCase {
    protected DataMapFile dmf;
    protected DataMap map;
    protected Project pr;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pr = new TstProject(new File("xyz"));
        map = new DataMap("m1");
        dmf = new DataMapFile(pr, map);
    }

    public void testGetObject() throws Exception {
    	assertSame(map, dmf.getObject());
    }
    
    public void testGetObjectName() throws Exception {
    	assertEquals(map.getName(), dmf.getObjectName());
    }
    
    public void testGetFileName() throws Exception {
    	assertEquals(map.getName() + ".map.xml", dmf.getLocation());
    }
}

