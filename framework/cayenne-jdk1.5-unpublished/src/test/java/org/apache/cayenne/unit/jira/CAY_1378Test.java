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
package org.apache.cayenne.unit.jira;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.unit.AccessStack;
import org.apache.cayenne.unit.CayenneCase;
import org.apache.cayenne.unit.CayenneResources;

public class CAY_1378Test extends CayenneCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        deleteTestData();
    }

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack("InheritanceFlatStack");
    }

    public void testFlattenedNullifyNullifyDeleteRules() throws Exception {

        DataContext context = createDataContext();

        User user = context.newObject(User.class);
        user.setName("test_user");
        Group group = context.newObject(Group.class);
        group.setName("test_group");
        group.addToGroupMembers(user);
        context.commitChanges();

        context.deleteObject(user);
        assertTrue(group.getGroupMembers().isEmpty());

        context.commitChanges();

        // here Cayenne would throw per CAY-1378 on an attempt to delete a previously
        // related transient object
        context.deleteObject(group);
        context.commitChanges();
    }
}
