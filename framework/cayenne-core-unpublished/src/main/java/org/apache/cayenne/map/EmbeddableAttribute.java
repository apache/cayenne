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
package org.apache.cayenne.map;

import java.io.Serializable;

import org.apache.cayenne.configuration.ConfigurationNode;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A persistent attribute of an embeddable object.
 * 
 * @since 3.0
 */
public class EmbeddableAttribute implements ConfigurationNode, XMLSerializable,
        Serializable {

    protected String name;
    protected String type;
    protected String dbAttributeName;

    protected Embeddable embeddable;

    public EmbeddableAttribute() {

    }

    public EmbeddableAttribute(String name) {
        this.name = name;
    }

    /**
     * @since 3.1
     */
    public <T> T acceptVisitor(ConfigurationNodeVisitor<T> visitor) {
        return visitor.visitEmbeddableAttribute(this);
    }

    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<embeddable-attribute name=\"" + getName() + '\"');

        if (getType() != null) {
            encoder.print(" type=\"");
            encoder.print(getType());
            encoder.print('\"');
        }

        // If this obj attribute is mapped to db attribute
        if (dbAttributeName != null) {
            encoder.print(" db-attribute-name=\"");
            encoder.print(Util.encodeXmlAttribute(dbAttributeName));
            encoder.print('\"');
        }

        encoder.println("/>");
    }

    public String getDbAttributeName() {
        return dbAttributeName;
    }

    public void setDbAttributeName(String dbAttributeName) {
        this.dbAttributeName = dbAttributeName;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public Embeddable getEmbeddable() {
        return embeddable;
    }

    public void setEmbeddable(Embeddable embeddable) {
        this.embeddable = embeddable;
    }
}
