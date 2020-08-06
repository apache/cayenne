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

package org.apache.cayenne.modeler.action;


import org.apache.cayenne.configuration.DataChannelDescriptor;
import org.apache.cayenne.configuration.DataNodeDescriptor;
import org.apache.cayenne.conn.DataSourceInfo;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class CreateNodeActionTest {

    @Test
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

        DataChannelDescriptor domain = new DataChannelDescriptor();
        domain.setName("aa");
        DataNodeDescriptor node = action.buildDataNode(domain);

        assertNotNull(node);
        assertNotNull(node.getName());

        DataSourceInfo ds1 = new DataSourceInfo();
        node.setDataSourceDescriptor(ds1);

        assertSame("Project DataNode must not wrap the DataSource", ds1, node
                .getDataSourceDescriptor());
    }
}
