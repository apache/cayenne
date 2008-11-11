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

import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataNodeConfigInfoTest extends CayenneCase {
	protected DataNodeConfigInfo test; 

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        test = new DataNodeConfigInfo();
    }
    
    public void testAdapter() throws Exception {
    	test.setAdapter("abc");
    	assertEquals("abc", test.getAdapter());
    }

    public void testDomain() throws Exception {
        test.setDomain("abc");
        assertEquals("abc", test.getDomain());
    }
    
    public void testDataSource() throws Exception {
        test.setDataSource("abc");
        assertEquals("abc", test.getDataSource());
    }
    
    public void testDriverFile() throws Exception {
    	File f = new File("abc");
        test.setDriverFile(f);
        assertSame(f, test.getDriverFile());
    }
    
    public void testName() throws Exception {
        test.setName("abc");
        assertEquals("abc", test.getName());
    }
}
