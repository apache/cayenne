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
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.project.ProjectPath;

/**
 * @author Andrus Adamchik
 */
public class ObjAttributeValidatorTest extends ValidatorTestBase {

    public void testValidateObjAttributes() throws Exception {
        DataDomain d1 = new DataDomain("d1");

        DataMap m1 = new DataMap("m1");
        d1.addMap(m1);
        ObjAttribute oa1 = buildValidObjAttribute(m1, "a1");
        validator.reset();
        new ObjAttributeValidator().validateObject(
            new ProjectPath(new Object[] { project, d1, m1, oa1.getEntity(), oa1 }),
            validator);
        assertValidator(ValidationInfo.VALID);

        oa1.setDbAttributePath(null);
        validator.reset();
        new ObjAttributeValidator().validateObject(
            new ProjectPath(new Object[] { project, d1, m1, oa1.getEntity(), oa1 }),
            validator);
        assertValidator(ValidationInfo.WARNING);
    }
}
