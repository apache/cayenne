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
package org.apache.cayenne.map;

import org.apache.cayenne.query.DefaultEmbeddableResultSegment;
import org.apache.cayenne.reflect.ClassDescriptor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * A metadata object that defines how a row in a result set can be converted to
 * result objects. SQLResult can be mapped to a single scalar, a single entity
 * or a mix of scalars and entities that is represented as an Object[].
 * 
 * @since 3.0
 */
public class SQLResult {

    protected String name;
    protected List<Object> resultDescriptors;

    /**
     * Creates an unnamed SQLResultSet.
     */
    public SQLResult() {

    }

    public List<Object> getResolvedComponents(EntityResolver resolver) {

        if (resultDescriptors == null) {
            return Collections.emptyList();
        }

        List<Object> resolvedComponents = new ArrayList<>(resultDescriptors.size());

        int offset = 0;
        for (Object component : getComponents()) {
            if (component instanceof String) {
                resolvedComponents.add(new DefaultScalarResultSegment((String) component, offset));
                offset = offset + 1;
            } else if (component instanceof EntityResult) {
                EntityResult entityResult = (EntityResult) component;
                Map<String, String> fields = entityResult.getDbFields(resolver);

                String entityName = entityResult.getEntityName();
                if (entityName == null) {
                    entityName = resolver.getObjEntity(entityResult.getEntityClass()).getName();
                }

                ClassDescriptor classDescriptor = resolver.getClassDescriptor(entityName);
                resolvedComponents.add(new DefaultEntityResultSegment(classDescriptor, fields, offset));
                offset = offset + fields.size();
            } else if (component instanceof EmbeddedResult) {
                EmbeddedResult embeddedResult = (EmbeddedResult)component;
                Map<String, String> fields = embeddedResult.getFields();
                resolvedComponents.add(new DefaultEmbeddableResultSegment(embeddedResult.getEmbeddable(), fields, offset));
                offset = offset + fields.size();
            } else {
                throw new IllegalArgumentException("Unsupported result descriptor component: " + component);
            }
        }

        return resolvedComponents;
    }

    /**
     * Creates a named SQLResultSet.
     */
    public SQLResult(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Returns a list of "uncompiled" result descriptors. Column descriptors are
     * returned as Strings, entity descriptors - as {@link EntityResult}. To get
     * fully resolved descriptors, use
     * {@link #getResolvedComponents(EntityResolver)}.
     */
    public List<Object> getComponents() {
        return resultDescriptors != null ? resultDescriptors : Collections.emptyList();
    }

    public void addEntityResult(EntityResult entityResult) {
        checkAndAdd(entityResult);
    }

    public void addEmbeddedResult(EmbeddedResult embeddedResult) {
        checkAndAdd(embeddedResult);
    }

    /**
     * Adds a result set column name to the mapping.
     */
    public void addColumnResult(String column) {
        checkAndAdd(column);
    }

    private void checkAndAdd(Object result) {
        if (resultDescriptors == null) {
            resultDescriptors = new ArrayList<>(3);
        }

        resultDescriptors.add(result);
    }
}
