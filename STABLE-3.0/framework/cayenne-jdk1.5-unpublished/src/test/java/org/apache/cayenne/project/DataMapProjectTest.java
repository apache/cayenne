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
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Iterator;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class DataMapProjectTest extends CayenneCase {
    protected DataMapProject p;
    protected File f;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        f = new File(getTestDir(), "Untitled.map.xml");
        if (f.exists()) {
            if (!f.delete()) {
                throw new RuntimeException("Can't delete file: " + f);
            }
        }

        // copy shared datamap to the test location
        DataMap m = getNode().getDataMaps().iterator().next();

        PrintWriter out = new PrintWriter(new FileOutputStream(f));

        try {
            m.encodeAsXML(out);
        } finally {
            out.close();
        }

        p = new DataMapProject(f);
    }
    
    public void testProjectFileForObject() throws Exception {
    	p.map = new DataMap("m1");
    	ProjectFile pf = p.projectFileForObject(p.map);
    	assertNull(pf);
    	
    	pf = p.projectFileForObject(p);
    	assertNotNull(pf);
    	assertTrue(pf instanceof DataMapFile);
    	assertSame(p.map, pf.getObject());
    }
    
    
    public void testConstructor() throws Exception {
        assertEquals(f.getCanonicalFile(), p.getMainFile());
        assertTrue(p.projectFileForObject(p) instanceof DataMapFile);
    }

    public void testTreeNodes() throws Exception {
        Iterator treeNodes = p.treeNodes();
        int len = 0;
        while (treeNodes.hasNext()) {
            len++;
            treeNodes.next();
        }

        assertTrue(len > 1);
    }
}
