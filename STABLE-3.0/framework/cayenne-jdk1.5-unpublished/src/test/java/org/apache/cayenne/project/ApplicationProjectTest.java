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
import java.util.List;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.DriverDataSourceFactory;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ApplicationProjectTest extends CayenneCase {
    protected ApplicationProject p;
    protected File f;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        f = new File(Configuration.DEFAULT_DOMAIN_FILE);
        p = new ApplicationProject(f);
    }

    public void testProjectFileForObject() throws Exception {
    	DataNode node = new DataNode("dn");
    	DataDomain dm = new DataDomain("dd");
    	dm.addNode(node);
    	p.getConfiguration().addDomain(dm);
    	
        ProjectFile pf = p.projectFileForObject(node);
        assertNull(pf);

        node.setDataSourceFactory(DriverDataSourceFactory.class.getName());
        ProjectFile pf1 = p.projectFileForObject(node);
        assertTrue(pf1 instanceof DataNodeFile);
        assertSame(node, pf1.getObject());
    }


    public void testConfig() throws Exception {
        assertNotNull(p.getConfiguration());
    }

    public void testConstructor() throws Exception {
        assertEquals(f.getCanonicalFile(), p.getMainFile());
        assertTrue(p.projectFileForObject(p) instanceof ApplicationProjectFile);

        assertNotNull(p.getChildren());
        assertEquals(0, p.getChildren().size());
    }

    public void testBuildFileList() throws Exception {
        // build a test project tree
        DataDomain d1 = new DataDomain("d1");
        DataMap m1 = new DataMap("m1");
        DataNode n1 = new DataNode("n1");
        n1.setDataSourceFactory(DriverDataSourceFactory.class.getName());

        d1.addMap(m1);
        d1.addNode(n1);

        ObjEntity oe1 = new ObjEntity("oe1");
        m1.addObjEntity(oe1);

        n1.addDataMap(m1);

        // initialize project 
        p.getConfiguration().addDomain(d1);

        // make assertions
        List files = p.buildFileList();

        assertNotNull(files);

        // list must have 3 files total
        assertEquals("Unexpected number of files: " + files, 3, files.size());
    }
}
