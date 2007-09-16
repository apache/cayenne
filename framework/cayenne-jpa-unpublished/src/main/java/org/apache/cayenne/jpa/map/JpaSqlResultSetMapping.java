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

import javax.persistence.SqlResultSetMapping;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaSqlResultSetMapping implements XMLSerializable {

    protected String name;
    protected Collection<JpaEntityResult> entityResults;
    protected Collection<JpaColumnResult> columnResults;

    public JpaSqlResultSetMapping() {

    }

    public JpaSqlResultSetMapping(SqlResultSetMapping annotation) {
        name = annotation.name();

        getEntityResults();
        for (int i = 0; i < annotation.entities().length; i++) {
            entityResults.add(new JpaEntityResult(annotation.entities()[i]));
        }

        getColumnResults();
        for (int i = 0; i < annotation.columns().length; i++) {
            columnResults.add(new JpaColumnResult(annotation.columns()[i]));
        }
    }
    
    public void encodeAsXML(XMLEncoder encoder) {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @TreeNodeChild(type = JpaColumnResult.class)
    public Collection<JpaColumnResult> getColumnResults() {
        if (columnResults == null) {
            columnResults = new ArrayList<JpaColumnResult>(5);
        }
        return columnResults;
    }

    @TreeNodeChild(type = JpaEntityResult.class)
    public Collection<JpaEntityResult> getEntityResults() {
        if (entityResults == null) {
            entityResults = new ArrayList<JpaEntityResult>(5);
        }

        return entityResults;
    }
}
