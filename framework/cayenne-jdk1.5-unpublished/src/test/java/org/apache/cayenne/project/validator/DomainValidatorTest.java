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
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class DomainValidatorTest extends ValidatorTestBase {

    public void testValidateDomains() throws Exception {
        // should succeed
        validator.reset();
        DataDomain d1 = new DataDomain("abc");
        new DomainValidator().validateObject(new ProjectPath(new Object[] { project, d1 }), validator);
        assertValidator(ValidationInfo.VALID);

        // should complain about duplicate name
        DataDomain d3 = new DataDomain("xyz");
        project.getConfiguration().addDomain(d3);
        project.getConfiguration().addDomain(d1);
        d3.setName(d1.getName());
        validator.reset();
        new DomainValidator().validateObject(new ProjectPath(new Object[] { project, d3 }), validator);
        assertValidator(ValidationInfo.ERROR);
    }

}
