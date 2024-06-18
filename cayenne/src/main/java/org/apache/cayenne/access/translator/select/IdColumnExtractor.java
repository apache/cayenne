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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.EntityResult;
import org.apache.cayenne.map.ObjEntity;

/**
 * @since 4.2
 */
class IdColumnExtractor extends BaseColumnExtractor {

    private final DbEntity dbEntity;
    private EntityResult result;

    IdColumnExtractor(TranslatorContext context, DbEntity dbEntity) {
        super(context);
        this.dbEntity = dbEntity;
    }

    IdColumnExtractor(TranslatorContext context, ObjEntity objEntity) {
        this(context, objEntity.getDbEntity());
        if(context.getQuery().needsResultSetMapping()) {
            this.result = new EntityResult(objEntity.getName());
        }
    }

    @Override
    public void extract(CayennePath prefix) {
        for (DbAttribute dba : dbEntity.getPrimaryKeys()) {
            addDbAttribute(prefix, prefix, dba);
            if(result != null) {
                result.addDbField(dba.getName(), prefix + dba.getName());
            }
        }
        if(result != null) {
            context.getSqlResult().addEntityResult(result);
        }
    }
}
