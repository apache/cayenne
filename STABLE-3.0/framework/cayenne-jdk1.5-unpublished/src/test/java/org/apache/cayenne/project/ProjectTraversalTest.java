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

import java.util.List;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.unit.CayenneCase;

/**
 */
public class ProjectTraversalTest extends CayenneCase {

    public void testTraverse1() throws Exception {
        TstProjectTraversalHelper helper = new TstProjectTraversalHelper();
        DataMap map = new DataMap("m1");
        new ProjectTraversal(helper).traverse(map);
        List view = helper.getNodes();
        assertNotNull(view);
        assertEquals(1, view.size());
        assertSame(map, view.get(0));
    }

    public void testTraverse2() throws Exception {
        TstProjectTraversalHelper helper = new TstProjectTraversalHelper();
        DataMap map = new DataMap("m1");
        ObjEntity ent = new ObjEntity("e1");
        map.addObjEntity(ent);
        new ProjectTraversal(helper).traverse(map);
        List view = helper.getNodes();
        assertNotNull(view);
        assertEquals(2, view.size());
        assertSame(map, view.get(0));
        assertSame(ent, view.get(1));
    }
    
    public void testTraverse3() throws Exception {
        TstProjectTraversalHelper helper = new TstProjectTraversalHelper();
        Project p = new DataMapProject(null);
        new ProjectTraversal(helper).traverse(p);
        List nodes = helper.getNodes();
        assertNotNull(nodes);
        assertEquals("Unexpected number of nodes.." + nodes, 2, nodes.size());
        assertSame(p, nodes.get(0));
    }
}
