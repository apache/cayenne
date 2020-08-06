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

package org.apache.cayenne.template;

import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.SQLStatement;
import org.apache.cayenne.access.jdbc.SQLTemplateProcessor;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.template.parser.Node;
import org.apache.cayenne.template.parser.ParseException;
import org.apache.cayenne.template.parser.SQLTemplateParser;
import org.apache.cayenne.template.parser.TokenMgrError;
import org.apache.cayenne.util.concurrentlinkedhashmap.ConcurrentLinkedHashMap;


/**
 * @since 4.1
 */
public class CayenneSQLTemplateProcessor implements SQLTemplateProcessor {

    ConcurrentLinkedHashMap<String, Node> templateCache = new ConcurrentLinkedHashMap
            .Builder<String, Node>().maximumWeightedCapacity(100).build();

    TemplateParserPool parserPool = new TemplateParserPool();

    private TemplateContextFactory contextFactory;

    public CayenneSQLTemplateProcessor(@Inject TemplateContextFactory contextFactory) {
        this.contextFactory = contextFactory;
    }

    @Override
    public SQLStatement processTemplate(String template, Map<String, ?> parameters) {
        Context context = contextFactory.createContext(parameters);
        return process(template, context);
    }

    @Override
    public SQLStatement processTemplate(String template, List<Object> positionalParameters) {
        Map<String, Object> parameters = new HashMap<>();
        int i=0;
        for(Object param : positionalParameters) {
            parameters.put(String.valueOf(i++), param);
        }
        Context context = contextFactory.createContext(parameters, true);
        return process(template, context);
    }

    protected SQLStatement process(String template, Context context) {
        Node node = templateCache.get(template);
        if(node == null) {
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

        return new SQLStatement(context.buildTemplate(), context.getColumnDescriptors(), context.getParameterBindings());
    }
}
