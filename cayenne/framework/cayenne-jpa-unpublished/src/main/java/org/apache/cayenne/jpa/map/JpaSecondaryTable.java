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

import javax.persistence.SecondaryTable;

import org.apache.cayenne.util.TreeNodeChild;
import org.apache.cayenne.util.XMLEncoder;

public class JpaSecondaryTable extends JpaTable {

    protected Collection<JpaPrimaryKeyJoinColumn> primaryKeyJoinColumns;

    public JpaSecondaryTable() {

    }

    public JpaSecondaryTable(SecondaryTable annotation) {
        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        if (!"".equals(annotation.catalog())) {
            catalog = annotation.catalog();
        }

        if (!"".equals(annotation.schema())) {
            schema = annotation.schema();
        }

        getUniqueConstraints();
        for (int i = 0; i < annotation.uniqueConstraints().length; i++) {
            uniqueConstraints.add(new JpaUniqueConstraint(
                    annotation.uniqueConstraints()[i]));
        }

        getPrimaryKeyJoinColumns();
        for (int i = 0; i < annotation.pkJoinColumns().length; i++) {
            primaryKeyJoinColumns.add(new JpaPrimaryKeyJoinColumn(annotation
                    .pkJoinColumns()[i]));
        }
    }

    @TreeNodeChild(type = JpaPrimaryKeyJoinColumn.class)
    public Collection<JpaPrimaryKeyJoinColumn> getPrimaryKeyJoinColumns() {
        if (primaryKeyJoinColumns == null) {
            primaryKeyJoinColumns = new ArrayList<JpaPrimaryKeyJoinColumn>();
        }

        return primaryKeyJoinColumns;
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<secondary-table");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (catalog != null) {
            encoder.print(" catalog=\"" + catalog + "\"");
        }

        if (schema != null) {
            encoder.print(" schema=\"" + schema + "\"");
        }
        
        encoder.println('>');
        encoder.indent(1);

        if (primaryKeyJoinColumns != null) {
            encoder.print(primaryKeyJoinColumns);
        }
        
        encoder.indent(-1);
        encoder.println("</secondary-table>");
    }
}
