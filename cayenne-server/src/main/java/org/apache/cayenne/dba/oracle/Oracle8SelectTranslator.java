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
package org.apache.cayenne.dba.oracle;

import java.sql.Connection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.JoinStack;
import org.apache.cayenne.query.Query;

/**
 * @since 3.0
 */
class Oracle8SelectTranslator extends OracleSelectTranslator {

    /**
     * @since 3.2
     */
    public Oracle8SelectTranslator(Query query, DataNode dataNode, Connection connection) {
        super(query, dataNode, connection);
    }
    
    /**
     * Returns an old style joint stack for Oracle8 that does not support explicit join
     * syntax.
     */
    @Override
    protected JoinStack createJoinStack() {
        return new Oracle8JoinStack(getAdapter(), queryMetadata.getDataMap(), this);
    }

}
