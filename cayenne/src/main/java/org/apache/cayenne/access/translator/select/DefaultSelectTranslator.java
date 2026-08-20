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
import org.apache.cayenne.access.translator.SelectTranslator;
import org.apache.cayenne.access.translator.TranslatedSelect;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.FluentSelect;
import org.apache.cayenne.query.Select;

/**
 * A {@link SelectTranslator} that translates a {@link FluentSelect} by running the {@link TranslationStage}
 * pipeline over a {@link SelectTranslatorContext}. This is the translator returned by the base
 * {@link org.apache.cayenne.dba.JdbcAdapter}; adapters may subclass it to customize translation.
 *
 * @since 5.0
 */
public class DefaultSelectTranslator implements SelectTranslator {

    @Override
    public TranslatedSelect translate(Select<?> query, DbAdapter adapter, EntityResolver resolver) {
        if (!(query instanceof FluentSelect)) {
            throw new CayenneRuntimeException("Unsupported type of Select query %s", query);
        }
        SelectTranslatorContext context = new SelectTranslatorContext(
                new FluentSelectWrapper((FluentSelect<?, ?>) query), adapter, resolver, null);
        context.translate();
        return context.getTranslation();
    }
}
