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
package org.apache.cayenne.dba.sqlserver;

import java.sql.Connection;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.SelectTranslator;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.QueryMetadata;

public class SQLServerSelectTranslator extends SelectTranslator {
	
    /**
     * @since 3.2
     */
    public SQLServerSelectTranslator(Query query, DataNode dataNode, Connection connection) {
        super(query, dataNode, connection);
    }
    
    @Override
    protected void appendLimitAndOffsetClauses(StringBuilder buffer) {
        QueryMetadata metadata = getQuery().getMetaData(getEntityResolver());
        
        int limit = metadata.getFetchLimit();
        int offset = metadata.getFetchOffset();
        
        if (limit > 0) {
        	String sql = buffer.toString();
        	
        	// If contains distinct insert top limit after
        	if (sql.startsWith("SELECT DISTINCT ")) {
        		buffer.replace(0, 15, "SELECT DISTINCT TOP " + (offset + limit));	
        		
        	} else {
        		buffer.replace(0, 6, "SELECT TOP " + (offset + limit));	
        	}
        }
    }

}