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
package org.apache.cayenne.reflect.generic;

import org.apache.cayenne.GenericPersistentObject;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.reflect.ClassDescriptor;
import org.apache.cayenne.reflect.SingletonFaultFactory;
import org.apache.cayenne.unit.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

public class PersistentObjectDescriptorFactory_VerticalInheritanceIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.INHERITANCE_VERTICAL_PROJECT);

    @Test
    public void visitProperties_IterationOrder() {

        PersistentObjectDescriptorFactory factory = new PersistentObjectDescriptorFactory(
                env.entityResolver().getClassDescriptorMap(),
                new SingletonFaultFactory(),
                new DefaultValueComparisonStrategyFactory(mock(ValueObjectTypeRegistry.class))
        );

        ClassDescriptor genStudent = factory.getDescriptor("GenStudent");
        assertNotNull(genStudent);
        ClassDescriptor genBoy = genStudent.getSubclassDescriptor("GenBoy");
        assertNotNull(genBoy);
        ClassDescriptor genGirl = genStudent.getSubclassDescriptor("GenGirl");
        assertNotNull(genGirl);

        assertNotSame(genStudent, genBoy);
        assertNotSame(genStudent, genGirl);
        assertNotSame(genBoy, genGirl);

        assertEquals(GenericPersistentObject.class, genBoy.getObjectClass());
        assertEquals(GenericPersistentObject.class, genGirl.getObjectClass());
        assertEquals(GenericPersistentObject.class, genStudent.getObjectClass());
    }
}
