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
package org.apache.cayenne.access;

import java.util.Collection;
import java.util.List;

import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.SQLAction;
import org.apache.cayenne.query.SQLActionVisitor;
import org.apache.cayenne.query.UpdateBatchQuery;

/**
 * Update query which contains information about DataDomain and creates special SQLAction
 */
class DataDomainUpdateQuery extends UpdateBatchQuery {
    DataDomain domain;

    public DataDomainUpdateQuery(DataDomain domain, DbEntity dbEntity,
            List<DbAttribute> qualifierAttributes, List<DbAttribute> updatedAttribute,
            Collection<String> nullQualifierNames, int batchCapacity) {
        super(dbEntity, qualifierAttributes, updatedAttribute, nullQualifierNames, batchCapacity);
        this.domain = domain;
    }

    public DataDomain getDomain() {
        return domain;
    }
    
    @Override
    public SQLAction createSQLAction(SQLActionVisitor visitor) {
        return new DataDomainActionBuilder(domain, visitor).batchAction(this);
    }
}
