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

import junit.framework.TestCase;

import org.apache.cayenne.access.DataNode;

/**
 */
public class DataNodeFileTest extends TestCase {

    protected DataNodeFile dnf;
    protected DataNode node;
    protected Project pr;

    @Override
    protected void setUp() throws Exception {
        pr = new TstProject(new File("xyz"));
        node = new DataNode("n1");
        dnf = new DataNodeFile(pr, node);
    }

    public void testGetObject() throws Exception {
        assertSame(node, dnf.getObject());
    }

    public void testGetObjectName() throws Exception {
        assertEquals(node.getName(), dnf.getObjectName());
    }

    public void testGetFileName() throws Exception {
        assertEquals(node.getName() + ".driver.xml", dnf.getLocation());
    }
}
