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

import java.net.URL;

import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.trans.QualifierTranslator;
import org.apache.cayenne.access.trans.QueryAssembler;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLAction;

/**
 * A flavor of OracleAdapter that implements workarounds for some old driver limitations.
 * 
 * @since 1.2
 */
public class Oracle8Adapter extends OracleAdapter {

    /**
     * Uses OracleActionBuilder to create the right action.
     */
    @Override
    public SQLAction getAction(Query query, DataNode node) {
        return query.createSQLAction(new Oracle8ActionBuilder(this, node
                .getEntityResolver()));
    }

    @Override
    protected URL findResource(String name) {

        if ("/types.xml".equals(name)) {
            name = "/types-oracle8.xml";
        }

        return super.findResource(name);
    }

    @Override
    public QualifierTranslator getQualifierTranslator(QueryAssembler queryAssembler) {
        return new Oracle8QualifierTranslator(queryAssembler);
    }
}
