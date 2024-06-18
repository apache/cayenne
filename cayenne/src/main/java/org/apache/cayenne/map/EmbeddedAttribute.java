/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.ConfigurationNodeVisitor;
import org.apache.cayenne.util.Util;
import org.apache.cayenne.util.XMLEncoder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * An attribute of the ObjEntity that maps to an embeddable class.
 * 
 * @since 3.0
 */
public class EmbeddedAttribute extends ObjAttribute {

    protected Map<String, String> attributeOverrides;

    public EmbeddedAttribute() {
        attributeOverrides = new HashMap<>();
    }

    public EmbeddedAttribute(String name) {
        this();
        setName(name);
    }

    public EmbeddedAttribute(String name, String type, ObjEntity entity) {
        this();
        setName(name);
        setType(type);
        setEntity(entity);
    }

    /**
     * Copying constructor
     * @param other attribute to copy
     * @since 4.2
     */
    public EmbeddedAttribute(EmbeddedAttribute other) {
        setName(other.getName());
        setType(other.getType());
        setEntity(other.getEntity());
        setDbAttributePathDirect(other.getDbAttributePath());
        setUsedForLocking(other.isUsedForLocking());
        setLazy(other.isLazy());
        attributeOverrides = other.getAttributeOverrides();
    }

    @Override
    public void encodeAsXML(XMLEncoder encoder, ConfigurationNodeVisitor delegate) {
        encoder.start("embedded-attribute")
                .attribute("name", getName())
                .attribute("type", getType());

        for (Map.Entry<String, String> e : attributeOverrides.entrySet()) {
            encoder.start("embeddable-attribute-override")
                    .attribute("name", e.getKey())
                    .attribute("db-attribute-path", e.getValue())
                    .end();
        }

        encoder.end();
    }

    public Map<String, String> getAttributeOverrides() {
        return Collections.unmodifiableMap(attributeOverrides);
    }

    public Embeddable getEmbeddable() {
        if (type == null) {
            return null;
        }

        return getNonNullNamespace().getEmbeddable(type);
    }

    private ObjAttribute makeObjAttribute(EmbeddableAttribute embeddableAttribute) {
        String dbPath = attributeOverrides.get(embeddableAttribute.getName());
        if (dbPath == null) {
            dbPath = embeddableAttribute.getDbAttributeName();
        }

        return makeObjAttribute(embeddableAttribute, dbPath);
    }

    private ObjAttribute makeObjAttribute(EmbeddableAttribute embeddableAttribute, String dbPath) {
        String fullName = getName() + "." + embeddableAttribute.getName();

        ObjAttribute oa = new ObjAttribute(fullName, embeddableAttribute.getType(), getEntity());
        oa.setDbAttributePath(dbPath);
        return oa;
    }

    /**
     * Returns an ObjAttribute that maps to a given {@link DbAttribute}, or returns null
     * if no such attribute exists.
     */
    public ObjAttribute getAttributeForDbPath(String dbPath) {

        Embeddable e = getEmbeddable();
        if (e == null) {
            return null;
        }

        EmbeddableAttribute ea = null;

        for (Map.Entry<String, String> override : attributeOverrides.entrySet()) {
            if (dbPath.equals(override.getValue())) {
                ea = e.getAttribute(override.getKey());
                break;
            }
        }

        if (ea == null) {
            ea = e.getAttributeForDbPath(dbPath);
        }

        if (ea != null) {
            return makeObjAttribute(ea, dbPath);
        }

        return null;
    }

    /**
     * Returns an ObjAttribute for a given name, taking into account column name
     * overrides.
     */
    public ObjAttribute getAttribute(String name) {
        Embeddable e = getEmbeddable();
        if (e == null) {
            return null;
        }

        EmbeddableAttribute ea = e.getAttribute(name);
        if (ea == null) {
            return null;
        }

        return makeObjAttribute(ea);
    }

    /**
     * Returns a Collection of ObjAttributes of an embedded object taking into account
     * column name overrides.
     */
    public Collection<ObjAttribute> getAttributes() {
        Embeddable e = getEmbeddable();
        if (e == null) {
            return Collections.emptyList();
        }

        Collection<EmbeddableAttribute> embeddableAttributes = e.getAttributes();
        Collection<ObjAttribute> objectAttributes = new ArrayList<>(embeddableAttributes.size());

        for (EmbeddableAttribute ea : embeddableAttributes) {
            objectAttributes.add(makeObjAttribute(ea));
        }

        return objectAttributes;
    }

    public void addAttributeOverride(String name, String dbAttributeName) {
        attributeOverrides.put(name, dbAttributeName);
    }

    public void removeAttributeOverride(String name) {
        attributeOverrides.remove(name);
    }

    /**
     * Returns a type of this attribute that must be an {@link Embeddable} object.
     */
    @Override
    public String getType() {
        return type;
    }

    /**
     * Returns Java class of an object property described by this attribute. Wraps any
     * thrown exceptions into CayenneRuntimeException.
     */
    @Override
    public Class<?> getJavaClass() {
        if (this.getType() == null) {
            return null;
        }

        try {
            return Util.getJavaClass(getType());
        }
        catch (ClassNotFoundException e) {
            throw new CayenneRuntimeException("Failed to load class for name '"
                    + this.getType()
                    + "': "
                    + e.getMessage(), e);
        }
    }

    /**
     * Sets a type of this attribute that must be an {@link Embeddable} object.
     */
    @Override
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Returns guaranteed non-null MappingNamespace of this relationship. If it happens to
     * be null, and exception is thrown. This method is intended for internal use by
     * Relationship class.
     */
    final MappingNamespace getNonNullNamespace() {

        if (entity == null) {
            throw new CayenneRuntimeException("Embedded attribute '%s' has no parent Entity.", getName());
        }

        return entity.getNonNullNamespace();
    }
}
