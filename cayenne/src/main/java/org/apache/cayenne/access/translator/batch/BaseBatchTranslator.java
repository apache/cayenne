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

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.DefaultSQLAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;
import org.apache.cayenne.query.BatchQueryRow;

import java.util.List;

/**
 * @param <T> type of the batch query to translate
 * @since 4.2
 */
public abstract class BaseBatchTranslator<T extends BatchQuery> implements BatchTranslator<T> {

    @Override
    public TranslatedBatch translate(T query, DbAdapter adapter) {
        BatchTranslatorContext<T> context = new BatchTranslatorContext<>(query, adapter);

        String sql = createSql(context);
        BatchParameterBinding[] bindings = createBindings(context);

        return new TranslatedBatch(sql, bindings, (template, row) -> updateBindings(context, template, row));
    }

    protected abstract String createSql(BatchTranslatorContext<T> context);

    protected BatchParameterBinding[] createBindings(BatchTranslatorContext<T> context) {
        return context.getBindings().stream()
                .map(b -> new BatchParameterBinding(b.jdbcType(), b.scale(), b.attribute()))
                .toArray(BatchParameterBinding[]::new);
    }

    protected abstract ParameterBinding[] updateBindings(
            BatchTranslatorContext<T> context,
            BatchParameterBinding[] template,
            BatchQueryRow row);

    protected String doTranslate(BatchTranslatorContext<T> context, NodeBuilder nodeBuilder) {
        Node node = context.getAdapter().getSqlTreeProcessor().process(nodeBuilder.build());

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultSQLAppendable(context));
        node.visit(visitor);

        return visitor.getSQLString();
    }

    protected abstract boolean isNullAttribute(BatchTranslatorContext<T> context, DbAttribute attribute);

    protected ExpressionNodeBuilder buildQualifier(BatchTranslatorContext<T> context, List<DbAttribute> attributeList) {
        ExpressionNodeBuilder eq = null;
        for (DbAttribute attr : attributeList) {
            Integer value = isNullAttribute(context, attr) ? null : 1;
            ExpressionNodeBuilder next = SQLBuilder
                    .column(attr.getName()).attribute(attr)
                    .eq(SQLBuilder.value(value).attribute(attr));
            if (eq == null) {
                eq = next;
            } else {
                eq = eq.and(next);
            }
        }
        return eq;
    }
}
