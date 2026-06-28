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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.jdbc.RSColumn;
import org.apache.cayenne.access.jdbc.PSParameter;
import org.apache.cayenne.access.translator.sqltemplate.directive.Directive;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.dba.DbAdapter;

/**
 * @since 4.1
 */
public class Context {

    private final StringBuilder builder;
    private final Map<String, ?> objects;
    private final Map<String, String> parameterAliases;
    private final Map<String, Directive> directives;
    private final DbAdapter adapter;

    private List<PSParameter> parameterBindings;
    private List<RSColumn> columnDescriptors;
    private int counter;

    public Context(Map<String, Directive> directives, Map<String, ?> parameters, boolean positionalMode,
                   DbAdapter adapter) {
        this.directives = directives;
        this.objects = parameters;
        this.adapter = adapter;
        this.builder = new StringBuilder();
        if(positionalMode) {
            parameterAliases = new HashMap<>();
        } else {
            parameterAliases = null;
        }
    }

    /**
     * Returns the JDBC type the target adapter prefers for binding the given type.
     *
     * @since 5.0
     */
    public int preferredBindingType(int jdbcType) {
        return adapter.preferredBindingType(jdbcType);
    }

    /**
     * Resolves the {@link ExtendedType} for the given Java class name via the target adapter. A null class name resolves
     * to the adapter's default type.
     *
     * @since 5.0
     */
    public ExtendedType<?> getExtendedType(String javaClassName) {
        return adapter.getExtendedTypes().getRegisteredType(javaClassName);
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

    public void addParameterBinding(int jdbcType, int scale, Object value) {
        if(parameterBindings == null) {
            parameterBindings = new ArrayList<>();
        }
        // a binding's statement position is its 1-based ordinal among the bound parameters; the
        // ExtendedType is resolved from the value via the adapter
        parameterBindings.add(new PSParameter(jdbcType, scale, null,
                parameterBindings.size() + 1, value, extendedType(value)));
    }

    private ExtendedType<?> extendedType(Object value) {
        return value != null
                ? adapter.getExtendedTypes().getRegisteredType(value.getClass())
                : adapter.getExtendedTypes().getDefaultType();
    }

    public void addColumnDescriptor(RSColumn descriptor) {
        if(columnDescriptors == null) {
            columnDescriptors = new ArrayList<>();
        }
        columnDescriptors.add(descriptor);
    }

    public RSColumn[] getColumnDescriptors() {
        if(columnDescriptors == null) {
            return new RSColumn[0];
        }
        return columnDescriptors.toArray(new RSColumn[columnDescriptors.size()]);
    }

    public PSParameter[] getParameterBindings() {
        if(parameterBindings == null) {
            return new PSParameter[0];
        }
        return parameterBindings.toArray(new PSParameter[parameterBindings.size()]);
    }
}
