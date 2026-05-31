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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Select;

/**
 * A {@link SelectTranslator} that translates a {@link FluentSelect} by running the {@link TranslationStage}
 * pipeline over a {@link TranslatorContext}. This is the translator returned by the base
 * {@link org.apache.cayenne.dba.JdbcAdapter}; adapters may subclass it to customize translation.
 *
 * @since 5.0
 */
public class DefaultSelectTranslator implements SelectTranslator {

    private static final TranslationStage[] TRANSLATION_STAGES = {
            new QualifierTranslationStage(),
            new ColumnExtractorStage(),
            new PrefetchNodeStage(),
            new OrderingStage(),
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

    @Override
    public TranslatedSelect translate(Select<?> query, DbAdapter adapter, EntityResolver resolver) {
        if (!(query instanceof FluentSelect)) {
            throw new CayenneRuntimeException("Unsupported type of Select query %s", query);
        }
        TranslatorContext context = new TranslatorContext(
                new FluentSelectWrapper((FluentSelect<?, ?>) query), adapter, resolver, null);
        translate(context);
        return context.toResult();
    }

    /**
     * Runs the {@link TranslationStage} pipeline over the given context. Used for the root query (by
     * {@link #translate(Select, DbAdapter, EntityResolver)}), and directly by {@link QualifierTranslator}
     * for subqueries (which consume the intermediate context rather than the final result).
     */
    static void translate(TranslatorContext context) {
        for (TranslationStage stage : TRANSLATION_STAGES) {
            stage.perform(context);
        }
    }
}
