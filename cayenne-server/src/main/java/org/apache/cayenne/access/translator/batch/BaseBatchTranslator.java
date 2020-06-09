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

import java.util.List;

import org.apache.cayenne.access.sqlbuilder.ExpressionNodeBuilder;
import org.apache.cayenne.access.sqlbuilder.NodeBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLBuilder;
import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.access.translator.select.DefaultQuotingAppendable;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.query.BatchQuery;

/**
 * @since 4.2
 * @param <T> type of the batch query to translate
 */
public abstract class BaseBatchTranslator<T extends BatchQuery> {

    protected final BatchTranslatorContext<T> context;

    protected DbAttributeBinding[] bindings;

    public BaseBatchTranslator(T query, DbAdapter adapter) {
        this.context = new BatchTranslatorContext<>(query, adapter);
    }

    public DbAttributeBinding[] getBindings() {
        return bindings;
    }

    /**
     * This method applies {@link org.apache.cayenne.access.translator.select.BaseSQLTreeProcessor} to the
     * provided SQL tree node and generates SQL string from it.
     *
     * @param nodeBuilder SQL tree node builder
     * @return SQL string
     */
    protected String doTranslate(NodeBuilder nodeBuilder) {
        Node node = nodeBuilder.build();
        // convert to database flavour
        node = context.getAdapter().getSqlTreeProcessor().process(node);
        // generate SQL
        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new DefaultQuotingAppendable(context));
        node.visit(visitor);

        bindings = context.getBindings().toArray(new DbAttributeBinding[0]);
        return visitor.getSQLString();
    }

    abstract protected boolean isNullAttribute(DbAttribute attribute);

    protected ExpressionNodeBuilder buildQualifier(List<DbAttribute> attributeList) {
        ExpressionNodeBuilder eq = null;
        for (DbAttribute attr : attributeList) {
            Integer value = isNullAttribute(attr) ? null : 1;
            ExpressionNodeBuilder next = SQLBuilder
                    .column(attr.getName()).attribute(attr)
                    .eq(SQLBuilder.value(value).attribute(attr));
            if(eq == null) {
                eq = next;
            } else {
                eq = eq.and(next);
            }
        }
        return eq;
    }
}
