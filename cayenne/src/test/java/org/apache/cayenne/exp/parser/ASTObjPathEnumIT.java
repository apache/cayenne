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
package org.apache.cayenne.exp.parser;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.testdo.enum_test.Enum1;
import org.apache.cayenne.testdo.enum_test.EnumEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

@UseCayenneRuntime(CayenneProjects.ENUM_PROJECT)
public class ASTObjPathEnumIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Test
    public void testInjectEnumByName() {
        ASTObjPath node = new ASTObjPath("enumAttribute");

        EnumEntity enumEntity = context.newObject(EnumEntity.class);
        assertNull(enumEntity.getEnumAttribute());

        node.injectValue(enumEntity, "one");
        assertEquals(Enum1.one, enumEntity.getEnumAttribute());
    }

    @Test(expected = IllegalArgumentException.class)
    public void testInjectUnknownEnumByName() {
        ASTObjPath node = new ASTObjPath("enumAttribute");

        EnumEntity enumEntity = context.newObject(EnumEntity.class);
        assertNull(enumEntity.getEnumAttribute());

        node.injectValue(enumEntity, "four");
    }
}
