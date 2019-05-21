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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.ColumnDescriptor;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.template.directive.Directive;

/**
 * @since 4.1
 */
public class Context {

    private final StringBuilder builder;
    private final Map<String, ?> objects;
    private final Map<String, String> parameterAliases;
    private final Map<String, Directive> directives;

    private List<ParameterBinding> parameterBindings;
    private List<ColumnDescriptor> columnDescriptors;
    private int counter;

    public Context(Map<String, Directive> directives, Map<String, ?> parameters, boolean positionalMode) {
        this.directives = directives;
        this.objects = parameters;
        this.builder = new StringBuilder();
        if(positionalMode) {
            parameterAliases = new HashMap<>();
        } else {
            parameterAliases = null;
        }
    }

    public Directive getDirective(String name) {
        return directives.get(name);
    }

    public StringBuilder getBuilder() {
        return builder;
    }

    public String buildTemplate() {
        if(parameterAliases != null) {
            // there is always helper object, thus -1
            if(counter < objects.size() - 1) {
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

        if(parameterAliases != null) {
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

    public void addParameterBinding(ParameterBinding binding) {
        if(parameterBindings == null) {
            parameterBindings = new ArrayList<>();
        }
        parameterBindings.add(binding);
    }

    public void addColumnDescriptor(ColumnDescriptor descriptor) {
        if(columnDescriptors == null) {
            columnDescriptors = new ArrayList<>();
        }
        columnDescriptors.add(descriptor);
    }

    public ColumnDescriptor[] getColumnDescriptors() {
        if(columnDescriptors == null) {
            return new ColumnDescriptor[0];
        }
        return columnDescriptors.toArray(new ColumnDescriptor[columnDescriptors.size()]);
    }

    public ParameterBinding[] getParameterBindings() {
        if(parameterBindings == null) {
            return new ParameterBinding[0];
        }
        return parameterBindings.toArray(new ParameterBinding[parameterBindings.size()]);
    }
}
