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
public class FlatProjectViewTest extends CayenneCase {

    public void testFlattenProjectView() throws Exception {
        DataMap map = new DataMap("m1");
        List view = new FlatProjectView().flattenProjectTree(map);
        assertNotNull(view);
        assertEquals(1, view.size());
        ProjectPath path10 = (ProjectPath)view.get(0);
        assertSame(map, path10.getPath()[0]);
        
        ObjEntity ent = new ObjEntity("e1");
        map.addObjEntity(ent);
        List view1 = new FlatProjectView().flattenProjectTree(map);
        assertNotNull(view1);
        assertEquals(2, view1.size());
        ProjectPath path21 = (ProjectPath)view1.get(1);
        assertSame(map, path21.getPath()[0]);
        assertSame(ent, path21.getPath()[1]);
    }
}
