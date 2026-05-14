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

import org.apache.cayenne.testdo.testmap.ArtGroup;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class CDOReflexiveRelDeleteIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    
    private ArtGroup parentGroup;
    private ArtGroup childGroup1;
    private ArtGroup childGroup2;
    private ArtGroup childGroup3;

    @BeforeEach
    public void setUp() throws Exception {

        parentGroup = env.context().newObject(ArtGroup.class);
        parentGroup.setName("parent");

        childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        childGroup2 = env.context().newObject(ArtGroup.class);
        childGroup2.setName("child2");
        childGroup2.setToParentGroup(parentGroup);

        childGroup3 = env.context().newObject(ArtGroup.class);
        childGroup3.setName("subchild");
        childGroup3.setToParentGroup(childGroup1);

        env.context().commitChanges();
    }

    // Test various delete orders. There are more possible literal combinations, but the
    // ones below fairly well
    // encompass the various orders that might be a problem. Add more if additional
    // problems come to light
    @Test
    public void reflexiveRelationshipDelete1() {
        env.context().deleteObjects(parentGroup);
        env.context().deleteObjects(childGroup1);
        env.context().deleteObjects(childGroup2);
        env.context().deleteObjects(childGroup3);
        env.context().commitChanges();
    }

    @Test
    public void reflexiveRelationshipDelete2() {
        env.context().deleteObjects(childGroup1);
        env.context().deleteObjects(parentGroup);
        env.context().deleteObjects(childGroup2);
        env.context().deleteObjects(childGroup3);
        env.context().commitChanges();
    }

    @Test
    public void reflexiveRelationshipDelete3() {
        env.context().deleteObjects(childGroup1);
        env.context().deleteObjects(childGroup3);
        env.context().deleteObjects(parentGroup);
        env.context().deleteObjects(childGroup2);
        env.context().commitChanges();
    }

    @Test
    public void reflexiveRelationshipDelete4() {
        env.context().deleteObjects(childGroup3);
        env.context().deleteObjects(parentGroup);
        env.context().deleteObjects(childGroup1);
        env.context().deleteObjects(childGroup2);
        env.context().commitChanges();
    }

}
