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

package org.apache.cayenne.project.validator;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class ObjEntityValidatorTest extends ValidatorTestBase {

    protected DataDomain domain;
    protected DataMap map;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        domain = new DataDomain("d1");

        map = new DataMap("m1");
        domain.addMap(map);

    }

    public void testValidateNoName() throws Exception {
        ObjEntity oe1 = new ObjEntity("oe1");
        DbEntity de1 = new DbEntity("de1");
        oe1.setDbEntityName("de1");
        oe1.setClassName("some.javaclass.name");
        map.addObjEntity(oe1);
        map.addDbEntity(de1);

        validator.reset();
        new ObjEntityValidator().validateObject(new ProjectPath(new Object[] {
                project, domain, map, oe1
        }), validator);
        assertValidator(ValidationInfo.VALID);

        // now remove the name
        oe1.setName(null);

        validator.reset();
        new ObjEntityValidator().validateObject(new ProjectPath(new Object[] {
                project, domain, map, oe1
        }), validator);
        assertValidator(ValidationInfo.ERROR);
    }
}
