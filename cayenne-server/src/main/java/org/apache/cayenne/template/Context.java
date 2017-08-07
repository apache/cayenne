/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.template;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.template.directive.Bind;

/**
 * @since 4.1
 */
public class Context {

    Map<String, Directive> directives = new HashMap<>();

    Map<String, Object> objects = new HashMap<>();

    List<ParameterBinding> parameterBindings = new ArrayList<>();

    List<ColumnDescriptor> columnDescriptors = new ArrayList<>();

    public Context() {
        directives.put("bind", new Bind());
    }

    public Directive getDirective(String name) {
        return directives.get(name);
    }

    public Object getObject(String name) {
        return objects.get(name);
    }

    public void addParameters(Map<String, ?> parameters) {
        objects.putAll(parameters);
    }

    public void addDirective(String name, Directive directive) {
        directives.put(name, directive);
    }

    public void addParameterBinding(ParameterBinding binding) {
        parameterBindings.add(binding);
    }

    public void addColumnDescriptor(ColumnDescriptor descriptor) {
        columnDescriptors.add(descriptor);
    }

    public ColumnDescriptor[] getColumnDescriptors() {
        return columnDescriptors.toArray(new ColumnDescriptor[0]);
    }

    public ParameterBinding[] getParameterBindings() {
        return parameterBindings.toArray(new ParameterBinding[0]);
    }
}
