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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.template.directive.Bind;
import org.apache.cayenne.template.directive.BindEqual;
import org.apache.cayenne.template.directive.BindNotEqual;
import org.apache.cayenne.template.directive.BindObjectEqual;
import org.apache.cayenne.template.directive.BindObjectNotEqual;
import org.apache.cayenne.template.directive.Directive;
import org.apache.cayenne.template.directive.Result;

/**
 * @since 4.1
 */
public class Context {

    Map<String, Directive> directives = new HashMap<>();

    Map<String, Object> objects = new HashMap<>();

    Map<String, String> parameterAliases;

    List<ParameterBinding> parameterBindings = new ArrayList<>();

    List<ColumnDescriptor> columnDescriptors = new ArrayList<>();

    StringBuilder builder = new StringBuilder();

    boolean positionalMode;

    int counter;

    public Context() {
        addDirective(             "result", Result.INSTANCE);
        addDirective(               "bind", Bind.INSTANCE);
        addDirective(          "bindEqual", BindEqual.INSTANCE);
        addDirective(       "bindNotEqual", BindNotEqual.INSTANCE);
        addDirective(    "bindObjectEqual", BindObjectEqual.INSTANCE);
        addDirective( "bindObjectNotEqual", BindObjectNotEqual.INSTANCE);

        addParameter("helper", new SQLTemplateRenderingUtils());
    }

    public Context(boolean positionalMode) {
        this();
        this.positionalMode = positionalMode;
        if(positionalMode) {
            parameterAliases = new HashMap<>();
        }
    }

    public Directive getDirective(String name) {
        return directives.get(name);
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    public String buildTemplate() {
        if(positionalMode) {
            if(counter <= objects.size() - 2) {
                throw new CayenneRuntimeException("Too many parameters to bind template: " + (objects.size() - 1));
            }
        }
        return builder.toString();
    }

    public boolean haveObject(String name) {
        return objects.containsKey(name);
    }

    public Object getObject(String name) {
        Object object = objects.get(name);
        if(object != null) {
            return object;
        }

        if(positionalMode) {
            String alias = parameterAliases.get(name);
            if(alias == null) {
                if(counter > objects.size() - 2) {
                    throw new CayenneRuntimeException("Too few parameters to bind template: " + (objects.size() - 1));
                }
                alias = String.valueOf(counter++);
                parameterAliases.put(name, alias);
            }
            // give next object on each invocation of method
            return objects.get(alias);
        }

        return null;
    }

    public void addParameter(String name, Object value) {
        objects.put(name, value);
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
