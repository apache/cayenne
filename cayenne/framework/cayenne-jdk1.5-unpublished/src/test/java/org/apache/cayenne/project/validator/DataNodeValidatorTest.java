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
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.project.ProjectPath;

/**
 */
public class DataNodeValidatorTest extends ValidatorTestBase {

    public void testValidateDataNodes() throws Exception {
        // should succeed
        DataDomain d1 = new DataDomain("abc");
        DataNode n1 = new DataNode("1");
        n1.setAdapter(new JdbcAdapter());
        n1.setDataSourceFactory("123");
        n1.setDataSourceLocation("qqqq");
        d1.addNode(n1);

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n1 }), validator);
        assertValidator(ValidationInfo.VALID);

        // should complain about no location
        DataNode n2 = new DataNode("2");
        n2.setAdapter(new JdbcAdapter());
        n2.setDataSourceFactory("123");
        d1.addNode(n2);

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n2 }), validator);
        assertValidator(ValidationInfo.ERROR);

        // should complain about duplicate name
        DataNode n3 = new DataNode("3");
        n3.setAdapter(new JdbcAdapter());
        n3.setDataSourceFactory("123");
        d1.addNode(n3);
        n3.setName(n1.getName());

        validator.reset();
        new DataNodeValidator().validateObject(new ProjectPath(new Object[] { project, d1, n3 }), validator);
        assertValidator(ValidationInfo.ERROR);
    }

}
