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
import org.apache.cayenne.map.compiled.CompiledColumn;
import org.apache.cayenne.map.compiled.CompiledTable;
import org.apache.cayenne.map.compiled.CompiledAttribute;
import org.apache.cayenne.map.compiled.CompiledEntity;
import org.apache.cayenne.query.EntityResult;

/**
 * @since 4.2
 */
class IdColumnExtractor extends BaseColumnExtractor {

    private final CompiledTable table;
    private CompiledEntity entity;
    private EntityResult result;

    IdColumnExtractor(SelectTranslatorContext context, CompiledTable table) {
        super(context);
        this.table = table;
        this.entity = null;
    }

    IdColumnExtractor(SelectTranslatorContext context, CompiledEntity entity) {
        this(context, entity.table());
        this.entity = entity;
        if(context.needsResultSetMapping()) {
            this.result = new EntityResult(entity.name());
        }
    }

    @Override
    public void extract(CayennePath prefix) {
        for (CompiledColumn column : table.primaryKey()) {
            ResultNodeDescriptor resultNodeDescriptor = addDbAttribute(prefix, prefix, column);
            if(result != null) {
                result.addDbField(column.name(), prefix + column.name());
            }
            if(entity != null) {
                // redefine PK type if there's a corresponding CompiledAttribute for it
                CompiledAttribute meaningfulPK = entity.attributeForColumn(column);
                if(meaningfulPK != null) {
                    resultNodeDescriptor.setJavaType(meaningfulPK.valueClassName());
                }
            }
        }
        if(result != null) {
            context.getSqlResult().addEntityResult(result);
        }
    }
}
