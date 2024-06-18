/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne;

import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CDOReflexiveRelDeleteIT extends RuntimeCase {

    @Inject
    private ObjectContext context;
    
    private ArtGroup parentGroup;
    private ArtGroup childGroup1;
    private ArtGroup childGroup2;
    private ArtGroup childGroup3;

    @Before
    public void setUp() throws Exception {

        parentGroup = context.newObject(ArtGroup.class);
        parentGroup.setName("parent");

        childGroup1 = context.newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        childGroup2 = context.newObject(ArtGroup.class);
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        childGroup3 = context.newObject(ArtGroup.class);
        childGroup3.setName("subchild");
        childGroup3.setToParentGroup(childGroup1);

        context.commitChanges();
    }

    // Test various delete orders. There are more possible literal combinations, but the
    // ones below fairly well
    // encompass the various orders that might be a problem. Add more if additional
    // problems come to light
    @Test
    public void testReflexiveRelationshipDelete1() {
        context.deleteObjects(parentGroup);
        context.deleteObjects(childGroup1);
        context.deleteObjects(childGroup2);
        context.deleteObjects(childGroup3);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipDelete2() {
        context.deleteObjects(childGroup1);
        context.deleteObjects(parentGroup);
        context.deleteObjects(childGroup2);
        context.deleteObjects(childGroup3);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipDelete3() {
        context.deleteObjects(childGroup1);
        context.deleteObjects(childGroup3);
        context.deleteObjects(parentGroup);
        context.deleteObjects(childGroup2);
        context.commitChanges();
    }

    @Test
    public void testReflexiveRelationshipDelete4() {
        context.deleteObjects(childGroup3);
        context.deleteObjects(parentGroup);
        context.deleteObjects(childGroup1);
        context.deleteObjects(childGroup2);
        context.commitChanges();
    }

}
