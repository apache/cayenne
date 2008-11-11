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

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ApplicationProjectFileTest extends CayenneCase {
    protected ApplicationProjectFile rpf;
    protected ProjectConfiguration conf;
    protected Project pr;

    /**
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        pr = new TstProject(new File("xyz"));
        conf = new ProjectConfiguration(new File("tstproj.123"));
        rpf = new ApplicationProjectFile(pr);
    }

    public void testGetObject() throws Exception {
    	assertSame(pr, rpf.getObject());
    }
    
    public void testGetObjectName() throws Exception {
    	assertEquals("cayenne", rpf.getObjectName());
    }
    
    public void testGetFileName() throws Exception {
    	assertEquals(Configuration.DEFAULT_DOMAIN_FILE, rpf.getLocation());
    }
}
