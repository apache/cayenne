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
import org.apache.cayenne.testdo.enum_test.Enum1;
import org.apache.cayenne.testdo.enum_test.EnumEntity;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class ASTObjPathEnumIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.ENUM_PROJECT);

    @Test
    public void injectEnumByName() {
        ASTObjPath node = new ASTObjPath("enumAttribute");

        EnumEntity enumEntity = env.context().newObject(EnumEntity.class);
        assertNull(enumEntity.getEnumAttribute());

        node.injectValue(enumEntity, "one");
        assertEquals(Enum1.one, enumEntity.getEnumAttribute());
    }

    @Test

    public void injectUnknownEnumByName() {
        assertThrows(IllegalArgumentException.class, () -> {

            ASTObjPath node = new ASTObjPath("enumAttribute");

            EnumEntity enumEntity = env.context().newObject(EnumEntity.class);
            assertNull(enumEntity.getEnumAttribute());

            node.injectValue(enumEntity, "four");
    
        });
    }

}
