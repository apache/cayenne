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

package org.apache.cayenne.unit;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.jdbc.reader.RowReaderFactory;
import org.apache.cayenne.dba.DbAdapter;

/**
 * Creates bare DataNodes for the tests that need a node as a key or a holder of an adapter, without the services of
 * a real node.
 */
public final class DataNodes {

    private DataNodes() {
    }

    public static DataNode named(String name) {
        return new DataNode(name, null, null, null, null, null, null, null, null, null, null, null);
    }

    public static DataNode withAdapter(DbAdapter adapter, RowReaderFactory rowReaderFactory) {
        return new DataNode(null, null, () -> adapter, null, null, rowReaderFactory, null, null, null, null, null,
                null);
    }
}
