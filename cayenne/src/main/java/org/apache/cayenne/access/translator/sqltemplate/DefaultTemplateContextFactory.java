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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.access.translator.sqltemplate.directive.Bind;
import org.apache.cayenne.access.translator.sqltemplate.directive.BindEqual;
import org.apache.cayenne.access.translator.sqltemplate.directive.BindNotEqual;
import org.apache.cayenne.access.translator.sqltemplate.directive.BindObjectEqual;
import org.apache.cayenne.access.translator.sqltemplate.directive.BindObjectNotEqual;
import org.apache.cayenne.access.translator.sqltemplate.directive.Directive;
import org.apache.cayenne.access.translator.sqltemplate.directive.Result;
import org.apache.cayenne.dba.DbAdapter;

/**
 * @since 4.1
 */
public class DefaultTemplateContextFactory implements TemplateContextFactory {

    private final SQLTemplateRenderingUtils helper;

    // directive map is static for now, can be easily injected
    private final Map<String, Directive> directives;

    public DefaultTemplateContextFactory() {
        Map<String, Directive> directiveHashMap = new HashMap<>();
        directiveHashMap.put("bind", Bind.INSTANCE);
        directiveHashMap.put("bindEqual", BindEqual.INSTANCE);
        directiveHashMap.put("bindNotEqual", BindNotEqual.INSTANCE);
        directiveHashMap.put("bindObjectEqual", BindObjectEqual.INSTANCE);
        directiveHashMap.put("bindObjectNotEqual", BindObjectNotEqual.INSTANCE);
        directiveHashMap.put("result", Result.INSTANCE);

        directives = Collections.unmodifiableMap(directiveHashMap);
        helper = new SQLTemplateRenderingUtils();
    }

    @Override
    public Context createContext(Map<String, ?> parameters, boolean positionalMode, DbAdapter adapter) {
        Map<String, Object> realParameters = new HashMap<>(parameters.size() + 1);
        realParameters.putAll(parameters);
        realParameters.put("helper", helper);
        return new Context(directives, realParameters, positionalMode, adapter);
    }
}
