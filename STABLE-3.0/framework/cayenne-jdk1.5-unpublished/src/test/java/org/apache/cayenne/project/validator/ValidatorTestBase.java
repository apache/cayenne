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

import java.io.File;
import java.sql.Types;

import junit.framework.TestCase;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbRelationship;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.project.ApplicationProject;

/**
 */
public abstract class ValidatorTestBase extends TestCase {

    protected static int counter = 1;

    protected Validator validator;
    protected ApplicationProject project;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        project = new ApplicationProject(new File(System.getProperty("user.dir")));
        validator = new Validator(project);
    }

    protected void assertValidator(int errorLevel) throws Exception {
        assertEquals(errorLevel, validator.getMaxSeverity());
    }

    protected DbRelationship buildValidDbRelationship(DataMap map, String name) {
        DbEntity src = new DbEntity("e1" + counter++);
        DbEntity target = new DbEntity("e2" + counter++);
        map.addDbEntity(src);
        map.addDbEntity(target);
        DbRelationship dr1 = new DbRelationship(name);
        dr1.setSourceEntity(src);
        dr1.setTargetEntity(target);
        src.addRelationship(dr1);
        return dr1;
    }

    protected ObjRelationship buildValidObjRelationship(DataMap map, String name) {
        DbRelationship dr1 = buildValidDbRelationship(map, "d" + name);

        ObjEntity src = new ObjEntity("ey" + counter++);
        src.setClassName("src");
        map.addObjEntity(src);
        src.setDbEntity((DbEntity) dr1.getSourceEntity());

        ObjEntity target = new ObjEntity("oey" + counter++);
        target.setClassName("target");
        map.addObjEntity(target);
        target.setDbEntity((DbEntity) dr1.getTargetEntity());

        ObjRelationship r1 = new ObjRelationship(name);
        r1.setTargetEntity(target);
        src.addRelationship(r1);

        r1.addDbRelationship(dr1);
        return r1;
    }

    protected ObjAttribute buildValidObjAttribute(DataMap map, String name) {
        DbAttribute a1 = new DbAttribute();
        a1.setName("d" + name);
        a1.setType(Types.CHAR);
        a1.setMaxLength(2);
        DbEntity e1 = new DbEntity("ex" + counter++);
        map.addDbEntity(e1);
        e1.addAttribute(a1);

        ObjEntity oe1 = new ObjEntity("oex" + counter++);
        map.addObjEntity(oe1);
        oe1.setDbEntity(e1);

        ObjAttribute oa1 = new ObjAttribute(name, "java.lang.Integer", oe1);
        oe1.addAttribute(oa1);
        oa1.setDbAttributePath(a1.getName());

        return oa1;
    }
}
