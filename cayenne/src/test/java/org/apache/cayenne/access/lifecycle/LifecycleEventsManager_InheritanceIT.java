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
package org.apache.cayenne.access.lifecycle;

import org.apache.cayenne.testdo.inheritance_flat.Group;
import org.apache.cayenne.testdo.inheritance_flat.Role;
import org.apache.cayenne.testdo.inheritance_flat.User;
import org.apache.cayenne.testdo.inheritance_flat.UserProperties;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class LifecycleEventsManager_InheritanceIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_SINGLE_TABLE1_PROJECT);

    @Test
    public void addListener_PostAdd_EntityInheritance() {
        LifecycleEventsManager lifecycleEventsManager = env.context().getChannel().getLifecycleEventsManager();

        StringBuilder callbackBuffer = new StringBuilder();
        lifecycleEventsManager.postAdd().addListener(
                u -> callbackBuffer.append("a:").append(u.getObjectId().getEntityName()).append(";"), User.class);

        env.context().newObject(User.class);
        assertEquals("a:User;", callbackBuffer.toString());

        // Group is a subclass of User
        callbackBuffer.setLength(0);
        env.context().newObject(Group.class);
        assertEquals("a:Group;", callbackBuffer.toString());

        callbackBuffer.setLength(0);
        env.context().newObject(Role.class);
        assertEquals("", callbackBuffer.toString());

        env.context().newObject(UserProperties.class);
        assertEquals("", callbackBuffer.toString());
    }
}
