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

import javax.persistence.FetchType;
import javax.persistence.OneToMany;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;

public class JpaOneToMany extends JpaRelationship {

    protected String mappedBy;
    protected Collection<JpaJoinColumn> joinColumns;
    protected JpaJoinTable joinTable;
    protected String orderBy;
    protected String mapKey;

    public JpaOneToMany() {

    }

    public JpaOneToMany(OneToMany annotation) {
        if (!Void.TYPE.equals(annotation.targetEntity())) {
            this.targetEntityName = annotation.targetEntity().getName();
        }

        for (int i = 0; i < annotation.cascade().length; i++) {
            if (cascade == null) {
                cascade = new JpaCascade();
            }
            cascade.getCascades().add(annotation.cascade()[i]);
        }

        fetch = annotation.fetch();
        mappedBy = annotation.mappedBy();
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {

        encoder.print("<one-to-many");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (targetEntityName != null) {
            encoder.print(" target-entity=\"" + targetEntityName + "\"");
        }

        if (fetch != null && fetch != FetchType.LAZY) {
            encoder.print(" fetch=\"" + fetch.name() + "\"");
        }

        if (mappedBy != null) {
            encoder.print(" mapped-by=\"" + mappedBy + "\"");
        }

        if (orderBy != null
                || mapKey != null
                || joinTable != null
                || (joinColumns != null && joinColumns.size() > 0)
                || cascade != null) {

            encoder.println('>');
            encoder.indent(1);

            if (orderBy != null) {
                encoder.print("<order-by>" + orderBy + "</order-by>");
            }

            if (mapKey != null) {
                encoder.print("<map-key name=\"" + mapKey + "\"/>");
            }

            if (joinTable != null) {
                joinTable.encodeAsXML(encoder);
            }

            for (JpaJoinColumn c : getJoinColumns()) {
                c.encodeAsXML(encoder);
            }

            if (cascade != null) {
                cascade.encodeAsXML(encoder);
            }

            encoder.indent(-1);
            encoder.println("</one-to-many>");
        }
        else {
            encoder.println("/>");
        }
    }

    @Override
    public boolean isToMany() {
        return true;
    }

    @TreeNodeChild(type = JpaJoinColumn.class)
    public Collection<JpaJoinColumn> getJoinColumns() {
        if (joinColumns == null) {
            joinColumns = new ArrayList<JpaJoinColumn>();
        }
        return joinColumns;
    }

    public JpaJoinTable getJoinTable() {
        return joinTable;
    }

    public void setJoinTable(JpaJoinTable joinTable) {
        this.joinTable = joinTable;
    }

    public String getOrderBy() {
        return orderBy;
    }

    public void setOrderBy(String orderBy) {
        this.orderBy = orderBy;
    }

    public String getMappedBy() {
        return mappedBy;
    }

    public void setMappedBy(String mappedBy) {
        this.mappedBy = mappedBy;
    }

    public String getMapKey() {
        return mapKey;
    }

    public void setMapKey(String mapKey) {
        this.mapKey = mapKey;
    }
}
