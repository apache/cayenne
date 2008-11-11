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

import javax.persistence.InheritanceType;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A JPA-compliant entity.
 * 
 */
public class JpaEntity extends JpaAbstractEntity implements XMLSerializable {

    protected String name;
    protected JpaTable table;
    protected JpaInheritance inheritance;
    protected String discriminatorValue;
    protected JpaDiscriminatorColumn discriminatorColumn;
    protected JpaSequenceGenerator sequenceGenerator;
    protected JpaTableGenerator tableGenerator;
    protected Collection<JpaSqlResultSetMapping> sqlResultSetMappings;
    protected Collection<JpaAttributeOverride> attributeOverrides;
    protected Collection<JpaAssociationOverride> associationOverrides;
    protected JpaEntity superEntity;

    // TODO: andrus, 7/25/2006 - according to the notes in the JPA spec FR, these
    // annotations can be specified on a mapped superclass as well as entity. Check the
    // spec test to verify that and move these to superclass.
    protected Collection<JpaNamedQuery> namedQueries;
    protected Collection<JpaNamedNativeQuery> namedNativeQueries;

    protected Collection<JpaSecondaryTable> secondaryTables;
    protected Collection<JpaPrimaryKeyJoinColumn> primaryKeyJoinColumns;

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<entity");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (className != null) {
            encoder.print(" class=\"" + className + "\"");
        }

        if (access != null) {
            encoder.print(" access=\"" + access.name() + "\"");
        }

        encoder.print(" metadata-complete=\"" + metadataComplete + "\"");
        encoder.println('>');
        encoder.indent(1);

        if (description != null) {
            encoder.println("<description>" + description + "</description>");
        }

        if (table != null) {
            table.encodeAsXML(encoder);
        }

        if (secondaryTables != null) {
            encoder.print(secondaryTables);
        }

        if (primaryKeyJoinColumns != null) {
            encoder.print(primaryKeyJoinColumns);
        }

        if (idClass != null) {
            idClass.encodeAsXML(encoder);
        }

        if (inheritance != null) {
            inheritance.encodeAsXML(encoder);
        }

        if (discriminatorValue != null) {
            encoder.println("<discriminator-value>"
                    + discriminatorValue
                    + "</discriminator-value>");
        }

        if (discriminatorColumn != null) {
            discriminatorColumn.encodeAsXML(encoder);
        }

        if (sequenceGenerator != null) {
            sequenceGenerator.encodeAsXML(encoder);
        }

        if (tableGenerator != null) {
            tableGenerator.encodeAsXML(encoder);
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

        if (excludeDefaultListeners) {
            encoder.println("<exclude-default-listeners/>");
        }

        if (excludeSuperclassListeners) {
            encoder.println("<exclude-superclass-listeners/>");
        }

        if (entityListeners != null) {
            entityListeners.encodeAsXML(encoder);
        }

        if (prePersist != null) {
            prePersist.encodeAsXML(encoder);
        }

        if (postPersist != null) {
            postPersist.encodeAsXML(encoder);
        }

        if (preRemove != null) {
            preRemove.encodeAsXML(encoder);
        }

        if (postRemove != null) {
            postRemove.encodeAsXML(encoder);
        }

        if (preUpdate != null) {
            preUpdate.encodeAsXML(encoder);
        }

        if (postUpdate != null) {
            postUpdate.encodeAsXML(encoder);
        }

        if (postLoad != null) {
            postLoad.encodeAsXML(encoder);
        }

        if (attributeOverrides != null) {
            encoder.print(attributeOverrides);
        }

        if (associationOverrides != null) {
            encoder.print(associationOverrides);
        }

        if (attributes != null) {
            attributes.encodeAsXML(encoder);
        }

        encoder.indent(-1);
        encoder.println("</entity>");
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @TreeNodeChild
    public JpaDiscriminatorColumn getDiscriminatorColumn() {
        return discriminatorColumn;
    }

    public void setDiscriminatorColumn(JpaDiscriminatorColumn discriminatorColumn) {
        this.discriminatorColumn = discriminatorColumn;
    }

    /**
     * Returns discriminatorValue property.
     * <h3>Specification Documentation</h3>
     * <p>
     * <b>Description:</b> An optional value that indicates that the row is an entity of
     * this entity type.
     * </p>
     * <p>
     * <b>Default:</b> If the DiscriminatorValue annotation is not specified, a
     * provider-specific function to generate a value representing the entity type is used
     * for the value of the discriminator column. If the DiscriminatorType is STRING, the
     * discriminator value default is the entity name.
     * </p>
     */
    public String getDiscriminatorValue() {
        return discriminatorValue;
    }

    public void setDiscriminatorValue(String discriminatorValue) {
        this.discriminatorValue = discriminatorValue;
    }

    /**
     * Returns a child JpaInheritance object that is only set on a root of inheritance
     * hierarchy.
     */
    @TreeNodeChild
    public JpaInheritance getInheritance() {
        return inheritance;
    }

    /**
     * Returns inheritance type for this entity hierarchy. If the entity has no
     * inheritance settings, returns null.
     */
    public InheritanceType lookupInheritanceStrategy() {

        if (inheritance != null) {
            return inheritance.getStrategy();
        }

        if (superEntity != null) {
            return superEntity.lookupInheritanceStrategy();
        }

        return null;
    }

    /**
     * Returns a table for this entity hierarchy.
     */
    public JpaTable lookupTable() {
        if (table != null) {
            return table;
        }

        if (superEntity != null) {
            return superEntity.lookupTable();
        }

        return null;
    }

    /**
     * Returns a discriminator column for this entity hierarchy.
     */
    public JpaDiscriminatorColumn lookupDiscriminatorColumn() {
        if (discriminatorColumn != null) {
            return discriminatorColumn;
        }

        if (superEntity != null) {
            return superEntity.lookupDiscriminatorColumn();
        }

        return null;
    }

    public void setInheritance(JpaInheritance inheritance) {
        this.inheritance = inheritance;
    }

    @TreeNodeChild
    public JpaSequenceGenerator getSequenceGenerator() {
        return sequenceGenerator;
    }

    public void setSequenceGenerator(JpaSequenceGenerator sequenceGenerator) {
        this.sequenceGenerator = sequenceGenerator;
    }

    @TreeNodeChild(type = JpaSqlResultSetMapping.class)
    public Collection<JpaSqlResultSetMapping> getSqlResultSetMappings() {
        if(sqlResultSetMappings == null) {
            sqlResultSetMappings = new ArrayList<JpaSqlResultSetMapping>();
        }
        
        return sqlResultSetMappings;
    }

    @TreeNodeChild
    public JpaTable getTable() {
        return table;
    }

    public void setTable(JpaTable table) {
        this.table = table;
    }

    @TreeNodeChild
    public JpaTableGenerator getTableGenerator() {
        return tableGenerator;
    }

    public void setTableGenerator(JpaTableGenerator tableGenerator) {
        this.tableGenerator = tableGenerator;
    }

    /**
     * Returns a collection of attribute overrides. Attribute overrides allows to change
     * the definition of attributes from a mapped superclass.
     */
    @TreeNodeChild(type = JpaAttributeOverride.class)
    public Collection<JpaAttributeOverride> getAttributeOverrides() {
        if (attributeOverrides == null) {
            attributeOverrides = new ArrayList<JpaAttributeOverride>();
        }

        return attributeOverrides;
    }

    @TreeNodeChild(type = JpaAssociationOverride.class)
    public Collection<JpaAssociationOverride> getAssociationOverrides() {
        if (associationOverrides == null) {
            associationOverrides = new ArrayList<JpaAssociationOverride>();
        }

        return associationOverrides;
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

    /**
     * Returns a collection of {@link JpaPrimaryKeyJoinColumn} objects that reference keys
     * of a primary table. PK join columns used by subclasses in a
     * {@link javax.persistence.InheritanceType#JOINED} mapping scenario.
     */
    @TreeNodeChild(type = JpaPrimaryKeyJoinColumn.class)
    public Collection<JpaPrimaryKeyJoinColumn> getPrimaryKeyJoinColumns() {
        if (primaryKeyJoinColumns == null) {
            primaryKeyJoinColumns = new ArrayList<JpaPrimaryKeyJoinColumn>();
        }
        return primaryKeyJoinColumns;
    }

    @TreeNodeChild(type = JpaSecondaryTable.class)
    public Collection<JpaSecondaryTable> getSecondaryTables() {
        if (secondaryTables == null) {
            secondaryTables = new ArrayList<JpaSecondaryTable>();
        }
        return secondaryTables;
    }

    public JpaSecondaryTable getSecondaryTable(String name) {
        if (secondaryTables != null) {
            for (JpaSecondaryTable table : secondaryTables) {
                if (name.equals(table.getName())) {
                    return table;
                }
            }
        }

        return null;
    }

    @Override
    public String toString() {
        return "JpaEntity:" + name;
    }

    public JpaEntity getSuperEntity() {
        return superEntity;
    }

    public void setSuperEntity(JpaEntity superEntity) {
        this.superEntity = superEntity;
    }
}
