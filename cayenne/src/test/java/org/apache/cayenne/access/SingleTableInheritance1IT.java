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
package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.Role;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * Special test cases per CAY-1378, CAY-1379.
 */
@UseCayenneRuntime(CayenneProjects.INHERITANCE_SINGLE_TABLE1_PROJECT)
public class SingleTableInheritance1IT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testGroupActions() throws Exception {

        User user = context.newObject(User.class);
        user.setName("test_user");

        Group group1 = context.newObject(Group.class);
        group1.setName("test_group1");

        Group group2 = context.newObject(Group.class);
        group2.setName("test_group2");

        group1.addToGroupMembers(user);
        group2.addToGroupMembers(group1);

        group2.getObjectContext().commitChanges();

        // Per CAY-1379 removing user and then refetching resulted in a FFE downstream
        group1.removeFromGroupMembers(user);
        ObjectSelect<Group> query = ObjectSelect.query(Group.class)
                .where(Role.ROLE_GROUPS.contains(group2));
        context.performQuery(query);
        context.commitChanges();

        context.deleteObjects(group1);
        context.deleteObjects(group2);
        context.deleteObjects(user);
        context.commitChanges();
    }

    @Test
    public void testFlattenedNullifyNullifyDeleteRules() throws Exception {

        User user = context.newObject(User.class);
        user.setName("test_user");
        Group group = context.newObject(Group.class);
        group.setName("test_group");
        group.addToGroupMembers(user);
        context.commitChanges();

        context.deleteObjects(user);
        assertTrue(group.getGroupMembers().isEmpty());

        context.commitChanges();

        // here Cayenne would throw per CAY-1378 on an attempt to delete a previously
        // related transient object
        context.deleteObjects(group);
        context.commitChanges();
    }
}
