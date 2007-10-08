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

package org.apache.cayenne.modeler.action;

import javax.sql.DataSource;

import junit.framework.TestCase;

import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;

import com.mockrunner.mock.jdbc.MockDataSource;

public class CreateNodeActionTst extends TestCase {

    public void testCreateDataNode() {
        CreateNodeAction action;

        try {
            action = new CreateNodeAction(null);
        }
        catch (InternalError e) {
            // caused by headless server running the tests ...
            // TODO: setup test environment DISPLAY variable
            return;
        }

        DataNode node = action.createDataNode(new DataDomain("DD"));

        assertNotNull(node);
        assertNotNull(node.getName());

        DataSource ds1 = new MockDataSource();
        node.setDataSource(ds1);

        assertSame("Project DataNode must not wrap the DataSource", ds1, node
                .getDataSource());
    }
}
