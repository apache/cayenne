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

import javax.persistence.JoinTable;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaJoinTable implements XMLSerializable {

    protected String name;
    protected String catalog;
    protected String schema;

    protected Collection<JpaJoinColumn> joinColumns;
    protected Collection<JpaJoinColumn> inverseJoinColumns;
    protected Collection<JpaUniqueConstraint> uniqueConstraints;

    public JpaJoinTable() {

    }

    public JpaJoinTable(JoinTable annotation) {
        name = annotation.name();
        catalog = annotation.catalog();
        schema = annotation.schema();

        getJoinColumns();
        for (int i = 0; i < annotation.joinColumns().length; i++) {
            joinColumns.add(new JpaJoinColumn(annotation.joinColumns()[i]));
        }

        getInverseJoinColumns();
        for (int i = 0; i < annotation.inverseJoinColumns().length; i++) {
            inverseJoinColumns.add(new JpaJoinColumn(annotation.inverseJoinColumns()[i]));
        }

        getUniqueConstraints();
        for (int i = 0; i < annotation.uniqueConstraints().length; i++) {
            uniqueConstraints.add(new JpaUniqueConstraint(
                    annotation.uniqueConstraints()[i]));
        }
    }

    public void encodeAsXML(XMLEncoder encoder) {
    }

    public String getCatalog() {
        return catalog;
    }

    public void setCatalog(String catalog) {
        this.catalog = catalog;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    @TreeNodeChild(type = JpaJoinColumn.class)
    public Collection<JpaJoinColumn> getInverseJoinColumns() {
        if (inverseJoinColumns == null) {
            inverseJoinColumns = new ArrayList<JpaJoinColumn>();
        }

        return inverseJoinColumns;
    }

    @TreeNodeChild(type = JpaJoinColumn.class)
    public Collection<JpaJoinColumn> getJoinColumns() {
        if (joinColumns == null) {
            joinColumns = new ArrayList<JpaJoinColumn>();
        }

        return joinColumns;
    }

    @TreeNodeChild(type = JpaUniqueConstraint.class)
    public Collection<JpaUniqueConstraint> getUniqueConstraints() {
        if (uniqueConstraints == null) {
            uniqueConstraints = new ArrayList<JpaUniqueConstraint>();
        }

        return uniqueConstraints;
    }
}
