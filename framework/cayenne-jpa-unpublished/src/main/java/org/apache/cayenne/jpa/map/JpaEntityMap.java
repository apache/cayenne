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

package org.apache.cayenne.jpa.map;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * An object that stores JPA mapping information. This is a root object in the hierarchy
 * defined in the <em>orm_1_0.xsd</em> schema.
 * 
 */
public class JpaEntityMap implements XMLSerializable {

    // mapped properties
    protected String version;
    protected String description;
    protected String packageName;
    protected String catalog;
    protected String schema;
    protected AccessType access;
    protected JpaPersistenceUnitMetadata persistenceUnitMetadata;

    protected Collection<JpaEntity> entities;
    protected Collection<JpaEmbeddable> embeddables;
    protected Collection<JpaMappedSuperclass> mappedSuperclasses;
    protected Collection<JpaNamedQuery> namedQueries;
    protected Collection<JpaNamedNativeQuery> namedNativeQueries;
    protected Collection<JpaSqlResultSetMapping> sqlResultSetMappings;
    protected Collection<JpaSequenceGenerator> sequenceGenerators;
    protected Collection<JpaTableGenerator> tableGenerators;

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<entity-mappings");
        if (version != null) {
            encoder.print(" version=\"" + version + "\"");
        }
        encoder.println('>');
        encoder.indent(1);

        if (description != null) {
            encoder.println("<description>" + description + "</description>");
        }

        if (persistenceUnitMetadata != null) {
            persistenceUnitMetadata.encodeAsXML(encoder);
        }

        if (packageName != null) {
            encoder.println("<package>" + packageName + "</package>");
        }

        if (schema != null) {
            encoder.println("<schema>" + schema + "</schema>");
        }

        if (catalog != null) {
            encoder.println("<catalog>" + catalog + "</catalog>");
        }

        if (access != null) {
            encoder.println("<access>" + access.name() + "</access>");
        }

        if (sequenceGenerators != null) {
            encoder.print(sequenceGenerators);
        }

        if (tableGenerators != null) {
            encoder.print(tableGenerators);
        }

        if (namedQueries != null) {
            encoder.print(namedQueries);
        }

        if (namedNativeQueries != null) {
            encoder.print(namedNativeQueries);
        }

        if (sqlResultSetMappings != null) {
            encoder.print(sqlResultSetMappings);
        }

        if (mappedSuperclasses != null) {
            encoder.print(mappedSuperclasses);
        }

        if (entities != null) {
            encoder.print(entities);
        }

        if (embeddables != null) {
            encoder.print(embeddables);
        }

        encoder.indent(-1);
        encoder.print("</entity-mappings>");
    }

    public JpaEntity getEntity(String className) {
        if (className == null) {
            throw new IllegalArgumentException("Null class name");
        }

        if (entities != null) {
            for (JpaEntity object : entities) {
                if (className.equals(object.getClassName())) {
                    return object;
                }
            }
        }

        return null;
    }

    /**
     * Returns an existing managed class, or null if no match is found.
     */
    public JpaManagedClass getManagedClass(String className) {
        if (className == null) {
            throw new IllegalArgumentException("Null class name");
        }

        if (mappedSuperclasses != null) {
            for (JpaMappedSuperclass object : mappedSuperclasses) {
                if (className.equals(object.getClassName())) {
                    return object;
                }
            }
        }

        if (entities != null) {
            for (JpaEntity object : entities) {
                if (className.equals(object.getClassName())) {
                    return object;
                }
            }
        }

        if (embeddables != null) {
            for (JpaEmbeddable object : embeddables) {
                if (className.equals(object.getClassName())) {
                    return object;
                }
            }
        }

        return null;
    }

    /**
     * Compiles and returns a map of managed class descriptors that includes descriptors
     * for entities, managed superclasses and embeddables. Note that class name key in the
     * map uses slashes, not dots, to separate package components.
     */
    public Map<String, JpaClassDescriptor> getManagedClasses() {
        Map<String, JpaClassDescriptor> managedClasses = new HashMap<String, JpaClassDescriptor>();

        if (mappedSuperclasses != null) {
            for (JpaMappedSuperclass object : mappedSuperclasses) {
                managedClasses.put(object.getClassName(), object.getClassDescriptor());
            }
        }

        if (entities != null) {
            for (JpaEntity object : entities) {
                managedClasses.put(object.getClassName(), object.getClassDescriptor());
            }
        }

        if (embeddables != null) {
            for (JpaEmbeddable object : embeddables) {
                managedClasses.put(object.getClassName(), object.getClassDescriptor());
            }
        }

        return managedClasses;
    }

    /**
     * Returns a JpaEntity describing a given persistent class.
     */
    public JpaEntity entityForClass(Class<?> entityClass) {

        if (entityClass == null) {
            throw new IllegalArgumentException("Null entity class");
        }

        return entityForClass(entityClass.getName());
    }

    /**
     * Returns a JpaEntity describing a given persistent class.
     */
    public JpaEntity entityForClass(String entityClassName) {
        if (entityClassName == null) {
            throw new IllegalArgumentException("Null entity class name");
        }

        if (entities == null) {
            return null;
        }

        for (JpaEntity entity : entities) {
            if (entityClassName.equals(entity.getClassName())) {
                return entity;
            }
        }

        return null;
    }

    /**
     * Returns a JpaEmbeddable describing a given embeddable class.
     */
    public JpaEmbeddable embeddableForClass(Class<?> embeddableClass) {

        if (embeddableClass == null) {
            throw new IllegalArgumentException("Null embeddable class");
        }

        return embeddableForClass(embeddableClass.getName());
    }

    /**
     * Returns a JpaEmbeddable describing a given embeddable class.
     */
    public JpaEmbeddable embeddableForClass(String embeddableClassName) {
        if (embeddableClassName == null) {
            throw new IllegalArgumentException("Null embeddable class name");
        }

        if (embeddables == null) {
            return null;
        }

        for (JpaEmbeddable embeddable : embeddables) {
            if (embeddableClassName.equals(embeddable.getClassName())) {
                return embeddable;
            }
        }

        return null;
    }

    public AccessType getAccess() {
        return access;
    }

    public void setAccess(AccessType access) {
        this.access = access;
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getPackageName() {
        return packageName;
    }

    public void setPackageName(String packageProperty) {
        this.packageName = packageProperty;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @TreeNodeChild(type = JpaEmbeddable.class)
    public Collection<JpaEmbeddable> getEmbeddables() {
        if (embeddables == null) {
            embeddables = new ArrayList<JpaEmbeddable>();
        }

        return embeddables;
    }

    @TreeNodeChild(type = JpaEntity.class)
    public Collection<JpaEntity> getEntities() {
        if (entities == null) {
            entities = new ArrayList<JpaEntity>();
        }

        return entities;
    }

    @TreeNodeChild(type = JpaMappedSuperclass.class)
    public Collection<JpaMappedSuperclass> getMappedSuperclasses() {
        if (mappedSuperclasses == null) {
            mappedSuperclasses = new ArrayList<JpaMappedSuperclass>();
        }

        return mappedSuperclasses;
    }

    @TreeNodeChild(type = JpaNamedNativeQuery.class)
    public Collection<JpaNamedNativeQuery> getNamedNativeQueries() {
        if (namedNativeQueries == null) {
            namedNativeQueries = new ArrayList<JpaNamedNativeQuery>();
        }

        return namedNativeQueries;
    }

    @TreeNodeChild(type = JpaNamedQuery.class)
    public Collection<JpaNamedQuery> getNamedQueries() {
        if (namedQueries == null) {
            namedQueries = new ArrayList<JpaNamedQuery>();
        }

        return namedQueries;
    }

    @TreeNodeChild(type = JpaSequenceGenerator.class)
    public Collection<JpaSequenceGenerator> getSequenceGenerators() {
        if (sequenceGenerators == null) {
            sequenceGenerators = new ArrayList<JpaSequenceGenerator>();
        }

        return sequenceGenerators;
    }

    @TreeNodeChild(type = JpaSqlResultSetMapping.class)
    public Collection<JpaSqlResultSetMapping> getSqlResultSetMappings() {
        if (sqlResultSetMappings == null) {
            sqlResultSetMappings = new ArrayList<JpaSqlResultSetMapping>();
        }

        return sqlResultSetMappings;
    }

    @TreeNodeChild(type = JpaTableGenerator.class)
    public Collection<JpaTableGenerator> getTableGenerators() {
        if (tableGenerators == null) {
            tableGenerators = new ArrayList<JpaTableGenerator>();
        }

        return tableGenerators;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @TreeNodeChild
    public JpaPersistenceUnitMetadata getPersistenceUnitMetadata() {
        return persistenceUnitMetadata;
    }

    public void setPersistenceUnitMetadata(
            JpaPersistenceUnitMetadata persistenceUnitMetadata) {
        this.persistenceUnitMetadata = persistenceUnitMetadata;
    }
}
