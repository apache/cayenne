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
package org.apache.cayenne.access.dbsync;

import org.apache.cayenne.access.DataNode;

/**
 * A SchemaUpdateStrategy that does nothing. This is usually the default strategy, as in
 * most cases DB schema management is outside the scope of Cayenne.
 * 
 * @since 3.0
 */
public class SkipSchemaUpdateStrategy implements SchemaUpdateStrategy {

    /**
     * @since 3.0
     */
    public void updateSchema(DataNode dataNode) {
        // does nothing
    }
}
