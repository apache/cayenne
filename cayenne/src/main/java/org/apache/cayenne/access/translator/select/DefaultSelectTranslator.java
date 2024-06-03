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

import java.util.Collections;
import java.util.Map;
import java.util.Objects;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.DbAttributeBinding;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.query.FluentSelect;

/**
 * Default translator of select queries {@link FluentSelect}.
 *
 * @since 4.2
 */
public class DefaultSelectTranslator implements SelectTranslator {

    private static final TranslationStage[] TRANSLATION_STAGES = {
            new ColumnExtractorStage(),
            new PrefetchNodeStage(),
            new OrderingStage(),
            new QualifierTranslationStage(),
            new HavingTranslationStage(),
            new OrderingGroupByStage(),
            new GroupByStage(),
            new DistinctStage(),
            new OrderingDistinctStage(),
            new LimitOffsetStage(),
            new ColumnDescriptorStage(),
            new TableTreeQualifierStage(),
            new TableTreeStage(),
            new SQLResultStage(),
            new SQLGenerationStage()
    };

    private final TranslatorContext context;

    /**
     * Constructor for the subquery case
     */
    DefaultSelectTranslator(TranslatableQueryWrapper query, TranslatorContext parentContext) {
        Objects.requireNonNull(query, "Query is null");
        Objects.requireNonNull(parentContext, "Parent context is null");
        this.context = new TranslatorContext(query, parentContext.getAdapter(), parentContext.getResolver(), parentContext);
        // skip SQL translation stage for nested translators, it should be performed by root context only
        this.context.setSkipSQLGeneration(true);
    }

    /**
     * Constructor for the root query case
     */
    DefaultSelectTranslator(TranslatableQueryWrapper query, DbAdapter adapter, EntityResolver entityResolver) {
        Objects.requireNonNull(query, "Query is null");
        Objects.requireNonNull(adapter, "DbAdapter is null");
        Objects.requireNonNull(entityResolver, "EntityResolver is null");
        this.context = new TranslatorContext(query, adapter, entityResolver, null);
    }

    public DefaultSelectTranslator(FluentSelect<?, ?> query, DbAdapter adapter, EntityResolver entityResolver) {
        this(new FluentSelectWrapper(query), adapter, entityResolver);
    }

    TranslatorContext getContext() {
        return context;
    }

    void translate() {
        for(TranslationStage stage : TRANSLATION_STAGES) {
            stage.perform(context);
        }
    }

    @Override
    public String getSql() {
        translate();
        return context.getFinalSQL();
    }

    @Override
    public DbAttributeBinding[] getBindings() {
        return context.getBindings().toArray(new DbAttributeBinding[0]);
    }

    @Override
    public Map<ObjAttribute, ColumnDescriptor> getAttributeOverrides() {
        return Collections.emptyMap();
    }

    @Override
    public ColumnDescriptor[] getResultColumns() {
        return context.getColumnDescriptors().toArray(new ColumnDescriptor[0]);
    }

    @Override
    public boolean isSuppressingDistinct() {
        return context.isDistinctSuppression();
    }

    @Override
    public boolean hasJoins() {
        return context.getTableCount() > 1;
    }

}
