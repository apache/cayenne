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

package org.apache.cayenne.access.translator.batch;

import org.apache.cayenne.access.jdbc.PSBatchParameter;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.UpdateBuilder;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.TypesMapping;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQueryRow;
import org.apache.cayenne.query.DeleteBatchQuery;

import java.sql.Types;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.*;

/**
 * A delete {@link BatchTranslator} that performs a 'soft' delete (an UPDATE setting the 'deleted' field
 * to true) for entities that have a boolean 'deleted' field, and falls back to the regular SQL DELETE of
 * the {@link DeleteBatchTranslator} superclass for the rest. Bind it under the {@link BatchTranslator#DELETE}
 * name to enable soft deletes. The preferred way to do this is
 * {@code CoreModule.extend(binder).useSoftDeleteIfColumnPresent(columnName)} (see
 * {@link org.apache.cayenne.configuration.runtime.CoreModuleExtender#useSoftDeleteIfColumnPresent(String)}).
 *
 * @since 4.2
 */
public class SoftDeleteBatchTranslator extends DeleteBatchTranslator {

    public static final String DEFAULT_DELETED_FIELD_NAME = "DELETED";

    private final String deletedFieldName;

    public SoftDeleteBatchTranslator() {
        this(DEFAULT_DELETED_FIELD_NAME);
    }

    public SoftDeleteBatchTranslator(String deletedFieldName) {
        this.deletedFieldName = deletedFieldName;
    }

    protected boolean isHardDelete(DeleteBatchQuery query) {
        DbAttribute attr = query.getDbEntity().getAttribute(deletedFieldName);
        return attr == null || attr.getType() != Types.BOOLEAN;
    }

    @Override
    protected String createSql(BatchTranslatorContext<DeleteBatchQuery> context) {
        DeleteBatchQuery query = context.getQuery();
        if (isHardDelete(query)) {
            return super.createSql(context);
        }

        DbAttribute deleteAttribute = query.getDbEntity().getAttribute(deletedFieldName);
        UpdateBuilder updateBuilder = update(context.getRootDbEntity())
                .set(column(deletedFieldName).attribute(deleteAttribute)
                        .eq(SQLBuilder.value(true).attribute(deleteAttribute)))
                .where(buildQualifier(context, query.getDbAttributes()));

        return doTranslate(context, updateBuilder);
    }

    @Override
    protected PSParameter[] updateBindings(BatchTranslatorContext<DeleteBatchQuery> context,
                                           PSBatchParameter[] template, BatchQueryRow row) {
        DeleteBatchQuery deleteBatch = context.getQuery();
        if (isHardDelete(deleteBatch)) {
            return super.updateBindings(context, template, row);
        }

        PSParameter[] bindings = new PSParameter[template.length];

        // the 'deleted' flag is the first binding and stays constant across all rows of the batch
        DbAttribute deleteAttribute = deleteBatch.getDbEntity().getAttribute(deletedFieldName);
        String typeName = TypesMapping.getJavaBySqlType(deleteAttribute);
        ExtendedType extendedType = context.getAdapter().getExtendedTypes().getRegisteredType(typeName);
        bindings[0] = template[0].bind(Boolean.TRUE, 1, extendedType);

        // bindings[0] holds the constant 'deleted' flag, so qualifier values start at position 1
        for(int i=0, position=1; i<deleteBatch.getDbAttributes().size(); i++) {
            position = updateBinding(context, template, bindings, row.getValue(i), position);
        }

        return bindings;
    }
}
