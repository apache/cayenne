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

import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.Role;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.unit.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Special test cases per CAY-1378, CAY-1379.
 */
public class SingleTableInheritance1IT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_SINGLE_TABLE1_PROJECT);

    @Test
    public void groupActions() throws Exception {

        User user = env.context().newObject(User.class);
        user.setName("test_user");

        Group group1 = env.context().newObject(Group.class);
        group1.setName("test_group1");

        Group group2 = env.context().newObject(Group.class);
        group2.setName("test_group2");

        group1.addToGroupMembers(user);
        group2.addToGroupMembers(group1);

        group2.getObjectContext().commitChanges();

        // Per CAY-1379 removing user and then refetching resulted in a FFE downstream
        group1.removeFromGroupMembers(user);
        ObjectSelect<Group> query = ObjectSelect.query(Group.class)
                .where(Role.ROLE_GROUPS.contains(group2));
        env.context().performQuery(query);
        env.context().commitChanges();

        env.context().deleteObjects(group1);
        env.context().deleteObjects(group2);
        env.context().deleteObjects(user);
        env.context().commitChanges();
    }

    @Test
    public void flattenedNullifyNullifyDeleteRules() throws Exception {

        User user = env.context().newObject(User.class);
        user.setName("test_user");
        Group group = env.context().newObject(Group.class);
        group.setName("test_group");
        group.addToGroupMembers(user);
        env.context().commitChanges();

        env.context().deleteObjects(user);
        assertTrue(group.getGroupMembers().isEmpty());

        env.context().commitChanges();

        // here Cayenne would throw per CAY-1378 on an attempt to delete a previously
        // related transient object
        env.context().deleteObjects(group);
        env.context().commitChanges();
    }
}
