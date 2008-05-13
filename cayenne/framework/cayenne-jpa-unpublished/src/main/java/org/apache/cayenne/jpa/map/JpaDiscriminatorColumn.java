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

import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaDiscriminatorColumn implements XMLSerializable {

    public static final int DEFAULT_LENGTH = 31;

    protected String name;
    protected DiscriminatorType discriminatorType = DiscriminatorType.STRING;
    protected String columnDefinition;
    protected int length;

    public JpaDiscriminatorColumn() {

    }

    public JpaDiscriminatorColumn(DiscriminatorColumn annotation) {
        if (!"".equals(annotation.name())) {
            name = annotation.name();
        }

        if (!"".equals(annotation.columnDefinition())) {
            columnDefinition = annotation.columnDefinition();
        }

        discriminatorType = annotation.discriminatorType();
        length = annotation.length();
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<discriminator-column");
        if (name != null) {
            encoder.print(" name=\"" + name + "\"");
        }

        if (discriminatorType != null && discriminatorType != DiscriminatorType.STRING) {
            encoder.print(" discriminator-type=\"" + discriminatorType.name() + "\"");
        }

        if (columnDefinition != null) {
            encoder.print(" column-definition=\"" + columnDefinition + "\"");
        }

        if (length > 0 && length != DEFAULT_LENGTH) {
            encoder.print(" length=\"" + length + "\"");
        }

        encoder.println("/>");
    }

    public String getColumnDefinition() {
        return columnDefinition;
    }

    public void setColumnDefinition(String columnDefinition) {
        this.columnDefinition = columnDefinition;
    }

    public DiscriminatorType getDiscriminatorType() {
        return discriminatorType;
    }

    public void setDiscriminatorType(DiscriminatorType discriminatrorType) {
        this.discriminatorType = discriminatrorType;
    }

    public int getLength() {
        return length;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
