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
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
/**
 * Some more tests regarding reflexive relationships, especially related to delete rules
 * etc. The implementation is hairy, and so needs a really good workout.
 */
public class CDOReflexiveRelIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void addDeleteNoCommit() {
        ArtGroup parentGroup = env.context().newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        env.context().deleteObjects(parentGroup);
    }

    @Test
    public void add() {
        ArtGroup parentGroup = env.context().newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);

        env.context().commitChanges();
    }

    @Test
    public void addDeleteWithCommit() {
        ArtGroup parentGroup = env.context().newObject(ArtGroup.class);
        parentGroup.setName("parent");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        env.context().commitChanges();

        env.context().deleteObjects(parentGroup);
        env.context().commitChanges();
    }

    @Test
    public void replaceDeleteNoCommit() {
        ArtGroup parentGroup1 = env.context().newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = env.context().newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);

        childGroup1.setToParentGroup(parentGroup2);

        env.context().deleteObjects(parentGroup1);
        env.context().deleteObjects(parentGroup2);
    }

    @Test
    public void replaceDeleteWithCommit() {
        ArtGroup parentGroup1 = env.context().newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = env.context().newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);
        childGroup1.setToParentGroup(parentGroup2);
        env.context().commitChanges();

        env.context().deleteObjects(parentGroup1);
        env.context().deleteObjects(parentGroup2);
        env.context().commitChanges();
    }

    @Test
    public void commitReplaceCommit() {
        ArtGroup parentGroup1 = env.context().newObject(ArtGroup.class);
        parentGroup1.setName("parent1");
        ArtGroup parentGroup2 = env.context().newObject(ArtGroup.class);
        parentGroup2.setName("parent2");

        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup1);
        env.context().commitChanges();
        childGroup1.setToParentGroup(parentGroup2);
        env.context().commitChanges();
    }

    @Test
    public void complexInsertUpdateOrdering() {
        ArtGroup parentGroup = env.context().newObject(ArtGroup.class);
        parentGroup.setName("parent");
        env.context().commitChanges();

        // Check that the update and insert both work write
        ArtGroup childGroup1 = env.context().newObject(ArtGroup.class);
        childGroup1.setName("child1");
        childGroup1.setToParentGroup(parentGroup);
        env.context().commitChanges();

        childGroup1.setToParentGroup(null);
        env.context().commitChanges();
    }

}
