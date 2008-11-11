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

package org.apache.cayenne.unit;

import java.sql.Connection;

import org.apache.cayenne.access.DataNode;

/**
 * Superclass of test cases that require multiple DataNodes.
 * 
 */
public abstract class MultiNodeCase extends CayenneCase {

    static final String NODE1 = "map-db1";
    static final String NODE2 = "map-db2";

    public static final String MULTINODE_ACCESS_STACK = "MultiNodeStack";

    @Override
    protected AccessStack buildAccessStack() {
        return CayenneResources.getResources().getAccessStack(MULTINODE_ACCESS_STACK);
    }

    @Override
    public DataNode getNode() {
        throw new RuntimeException(
                "'getNode() makes no sense in multinode environment.. "
                        + "use getNode1() or getNode2()");
    }

    @Override
    public Connection getConnection() {
        throw new RuntimeException(
                "'getConnection() makes no sense in multinode environment.. "
                        + "obtain it via an appropraite DataNode.");
    }

    public DataNode getNode1() {
        return accessStack.getDataDomain().getNode(NODE1);
    }

    public DataNode getNode2() {
        return accessStack.getDataDomain().getNode(NODE2);
    }
}
