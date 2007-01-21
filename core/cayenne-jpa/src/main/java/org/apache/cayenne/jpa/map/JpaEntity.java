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

import org.apache.cayenne.util.TreeNodeChild;

/**
 * A JPA-compliant entity.
 * 
 * @author Andrus Adamchik
 */
public class JpaEntity extends JpaAbstractEntity {

    protected String name;
    protected JpaTable table;
    protected JpaInheritance inheritance;
    protected String discriminatorValue;
    protected JpaDiscriminatorColumn discriminatorColumn;
    protected JpaSequenceGenerator sequenceGenerator;
    protected JpaTableGenerator tableGenerator;
    protected JpaSqlResultSetMapping sqlResultSetMapping;
    protected Collection<JpaAttributeOverride> attributeOverrides;
    protected Collection<JpaAssociationOverride> associationOverrides;

    // TODO: andrus, 7/25/2006 - according to the notes in the JPA spec FR, these
    // annotations can be specified on a mapped superclass as well as entity. Check the
    // spec test to verify that and move these to superclass.
    protected Collection<JpaNamedQuery> namedQueries;
    protected Collection<JpaNamedNativeQuery> namedNativeQueries;

    protected Collection<JpaSecondaryTable> secondaryTables;
    protected Collection<JpaPrimaryKeyJoinColumn> primaryKeyJoinColumns;

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

    @TreeNodeChild
    public JpaInheritance getInheritance() {
        return inheritance;
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

    @TreeNodeChild
    public JpaSqlResultSetMapping getSqlResultSetMapping() {
        return sqlResultSetMapping;
    }

    public void setSqlResultSetMapping(JpaSqlResultSetMapping sqlResultSetMapping) {
        this.sqlResultSetMapping = sqlResultSetMapping;
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
            namedNativeQueries = new ArrayList();
        }
        return namedNativeQueries;
    }

    @TreeNodeChild(type = JpaNamedQuery.class)
    public Collection<JpaNamedQuery> getNamedQueries() {
        if (namedQueries == null) {
            namedQueries = new ArrayList();
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
            primaryKeyJoinColumns = new ArrayList();
        }
        return primaryKeyJoinColumns;
    }

    @TreeNodeChild(type = JpaSecondaryTable.class)
    public Collection<JpaSecondaryTable> getSecondaryTables() {
        if (secondaryTables == null) {
            secondaryTables = new ArrayList();
        }
        return secondaryTables;
    }

    @Override
    public String toString() {
        return "JpaEntity:" + name;
    }
}
