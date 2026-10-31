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

package org.apache.cayenne.access.translator.sqltemplate;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.translator.SQLTemplateTranslator;
import org.apache.cayenne.access.translator.TranslatedSQL;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.resolved.ResolvedSQL;
import org.apache.cayenne.access.translator.sqltemplate.parser.Node;
import org.apache.cayenne.access.translator.sqltemplate.parser.ParseException;
import org.apache.cayenne.access.translator.sqltemplate.parser.SQLTemplateParser;
import org.apache.cayenne.access.translator.sqltemplate.parser.TokenMgrError;
import org.apache.cayenne.util.ClockMap;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;


/**
 * @since 5.0
 */
public class DefaultSQLTemplateTranslator implements SQLTemplateTranslator {

    private final ClockMap<String, Node> templateCache;
    private final TemplateParserPool parserPool;
    private final TemplateContextFactory contextFactory;

    public DefaultSQLTemplateTranslator(@Inject TemplateContextFactory contextFactory) {
        this.contextFactory = contextFactory;
        this.templateCache = new ClockMap<>(100);
        this.parserPool = new TemplateParserPool();
    }

    @Override
    public TranslatedSQL translate(ResolvedSQL query, DbAdapter adapter) {
        String template = query.template(adapter);
        if (template == null) {
            throw new CayenneRuntimeException("No template string configured for adapter %s",
                    adapter.getClass().getName());
        }

        // bind either positional or named parameters
        Context context;
        if (query.positionalParams().isEmpty()) {
            context = contextFactory.createContext(query.params(), adapter);
        } else {
            Map<String, Object> parameters = new HashMap<>();
            int i = 0;
            for (Object param : query.positionalParams()) {
                parameters.put(String.valueOf(i++), param);
            }
            context = contextFactory.createContext(parameters, true, adapter);
        }

        return process(query, template, context);
    }

    protected TranslatedSQL process(ResolvedSQL query, String template, Context context) {
        Node node = templateCache.get(template);
        if (node == null) {
            SQLTemplateParser parser = parserPool.get();
            try {
                parser.ReInit(new ByteArrayInputStream(template.getBytes()));
                node = parser.template();
            } catch (ParseException | TokenMgrError ex) {
                throw new CayenneRuntimeException("Error parsing template '%s' : %s", template, ex.getMessage());
            } finally {
                parserPool.put(parser);
            }
            // can ignore case when someone resolved this template concurrently, it has no side effects
            templateCache.put(template, node);
        }

        node.evaluate(context);

        return new TranslatedSQL(query, context.buildTemplate(), context.getParameterBindings(),
                context.getColumnDescriptors());
    }
}
