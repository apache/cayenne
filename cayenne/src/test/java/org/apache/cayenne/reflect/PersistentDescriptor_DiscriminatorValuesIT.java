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

package org.apache.cayenne.reflect;

import org.apache.cayenne.testdo.inheritance_with_enum.Root;
import org.apache.cayenne.testdo.inheritance_with_enum.Sub;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PersistentDescriptor_DiscriminatorValuesIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_WITH_ENUM_PROJECT);

    @Test
    public void compiledFromQualifier() {
        // "type = 1" in the mapping, on a "short" attribute: the value is converted when the descriptor is built
        Sub sub = new Sub();
        env.context().getEntityResolver().getClassDescriptor("Sub").injectDiscriminatorValues(sub);
        assertEquals((short) 1, sub.getType());

        Root root = new Root();
        env.context().getEntityResolver().getClassDescriptor("Root").injectDiscriminatorValues(root);
        assertEquals((short) 0, root.getType());
    }

    @Test
    public void appliedToNewObjects() {
        assertEquals((short) 1, env.context().newObject(Sub.class).getType());
        assertEquals((short) 0, env.context().newObject(Root.class).getType());
    }
}
