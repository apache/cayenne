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
import java.util.Collection;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

/**
 * A mapping descriptor of an embeddable class. Embeddable is a persistent class that
 * doesn't have its own identity and is embedded in other persistent classes. It can be
 * viewed as a custom type mapped to one or more database columns. Embeddable mapping can
 * include optional default column names that can be overriden by the owning entity.
 * 
 * @since 3.0
 * @author Andrus Adamchik
 */
public class Embeddable implements XMLSerializable, Serializable {

    protected String className;
    protected SortedMap<String, EmbeddableAttribute> attributes;

    public Embeddable() {
        this(null);
    }

    public Embeddable(String className) {
        this.attributes = new TreeMap<String, EmbeddableAttribute>();
        this.className = className;
    }

    /**
     * Returns EmbeddableAttribute of this Embeddable that maps to
     * <code>dbAttribute</code> parameter. Returns null if no such attribute is found.
     */
    public EmbeddableAttribute getAttributeForDbPath(String dbPath) {
        for (EmbeddableAttribute attribute : attributes.values()) {
            if (dbPath.equals(attribute.getDbAttributeName())) {
                return attribute;
            }
        }

        return null;
    }

    /**
     * Returns an unmodifiable sorted map of embeddable attributes.
     */
    public SortedMap<String, EmbeddableAttribute> getAttributeMap() {
        // create a new instance ... Caching unmodifiable map causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableSortedMap(attributes);
    }

    /**
     * Returns an unmodifiable collection of embeddable attributes.
     */
    public Collection<EmbeddableAttribute> getAttributes() {
        // create a new instance. Caching unmodifiable collection causes
        // serialization issues (esp. with Hessian).
        return Collections.unmodifiableCollection(attributes.values());
    }

    /**
     * Adds new embeddable attribute to the entity, setting its parent embeddable to be
     * this object. If attribute has no name, IllegalArgumentException is thrown.
     */
    public void addAttribute(EmbeddableAttribute attribute) {
        if (attribute.getName() == null) {
            throw new IllegalArgumentException("Attempt to insert unnamed attribute.");
        }

        Object existingAttribute = attributes.get(attribute.getName());
        if (existingAttribute != null) {
            if (existingAttribute == attribute) {
                return;
            }
            else {
                throw new IllegalArgumentException(
                        "An attempt to override embeddable attribute '"
                                + attribute.getName()
                                + "'");
            }
        }

        attributes.put(attribute.getName(), attribute);
        attribute.setEmbeddable(this);
    }

    public EmbeddableAttribute getAttribute(String name) {
        return attributes.get(name);
    }

    public void removeAttribute(String name) {
        attributes.remove(name);
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    /**
     * {@link XMLSerializable} implementation that generates XML for embeddable.
     */
    public void encodeAsXML(XMLEncoder encoder) {
        encoder.print("<embeddable");
        if (getClassName() != null) {
            encoder.print(" className=\"");
            encoder.print(getClassName());
            encoder.print("\"");
        }
        encoder.println(">");

        encoder.indent(1);
        encoder.print(attributes);
        encoder.indent(-1);
        encoder.println("</embeddable>");
    }
}
