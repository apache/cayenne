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

package org.apache.cayenne.xml;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.PropertyUtils;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;

/**
 * A convenience class for dealing with the mapping file. This can encode and decode
 * objects based upon the schema given by the map file.
 * 
 * @since 1.2
 */
final class XMLMappingDescriptor {

    private SerializableEntity rootEntity;
    private Map<String, SerializableEntity> entities;
    private ObjectContext objectContext;

    /**
     * Creates new XMLMappingDescriptor using a URL that points to the mapping file.
     * 
     * @param mappingUrl A URL to the mapping file that specifies the mapping model.
     * @throws CayenneRuntimeException
     */
    XMLMappingDescriptor(String mappingUrl) throws CayenneRuntimeException {

        // Read in the mapping file.
        DocumentBuilder builder = XMLUtil.newBuilder();

        Document document;
        try {
            document = builder.parse(mappingUrl);
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error parsing XML at " + mappingUrl, ex);
        }

        Element root = document.getDocumentElement();

        if (!"model".equals(root.getNodeName())) {
            throw new CayenneRuntimeException(
                    "Root of the mapping model must be \"model\"");
        }

        Map<String, SerializableEntity> entities = new HashMap<String, SerializableEntity>();
        for (Element e : XMLUtil.getChildren(root)) {
            SerializableEntity entity = new SerializableEntity(this, e);
            String tag = e.getAttribute("xmlTag");
            entities.put(tag, entity);

            if (rootEntity == null) {
                rootEntity = entity;
            }
        }

        this.entities = entities;
    }

    SerializableEntity getRootEntity() {
        return rootEntity;
    }

    /**
     * Decodes the supplied DOM document into an object.
     * 
     * @param xml The DOM document containing the encoded object.
     * @return The decoded object.
     * @throws CayenneRuntimeException
     */
    Object decode(Element xml, ObjectContext objectContext) throws CayenneRuntimeException {

        // TODO: Add an error check to make sure the mapping file actually is for this
        // data file.

        // Store a local copy of the data context.
        this.objectContext = objectContext;
        
        // Create the object to be returned.
        Object ret = createObject(rootEntity.getDescriptor(), xml);

        // We want to read each value from the XML file and then set the corresponding
        // property value in the object to be returned.
        for (Element e : XMLUtil.getChildren(xml)) {
            decodeProperty(ret, rootEntity.getDescriptor(), e);
        }

        return ret;
    }

    /**
     * Returns the entity XML block with the same "xmlTag" value as the passed in name.
     * 
     * @param name The name of the entity to retrieve.
     * @return The entity with "xmlTag" equal to the passed in name.
     */
    SerializableEntity getEntity(String name) {
        return entities.get(name);
    }

    /**
     * Returns the property that is associated with the passed in XML tag.
     * 
     * @param entityMapping The root to which the reference to find is relative to.
     * @param propertyXmlTag The name of the entity.
     * @return A name of the Java property mapped for the XML tag.
     */
    private String getPropertyMappingName(Element entityMapping, String propertyXmlTag) {
        for (Element propertyMapping : XMLUtil.getChildren(entityMapping)) {
            if (propertyXmlTag.equals(propertyMapping.getAttribute("xmlTag"))) {
                return propertyMapping.getAttribute("name");
            }
        }

        return null;
    }

    /**
     * Decodes a property.
     * 
     * @param object The object to be updated with the decoded property's value.
     * @param entityMapping The entity block that contains the property mapping for the
     *            value.
     * @param propertyData The encoded property.
     * @throws CayenneRuntimeException
     */
    private void decodeProperty(Object object, Element entityMapping, Element propertyData)
            throws CayenneRuntimeException {

        String xmlTag = propertyData.getNodeName();
        String propertyName = getPropertyMappingName(entityMapping, xmlTag);

        // check unmapped data
        if (propertyName == null) {
            return;
        }

        SerializableEntity targetEntityMapping = getEntity(xmlTag);

        // This is a "simple" encoded property.
        if (targetEntityMapping == null) {
            setProperty(object, propertyName, XMLUtil.getText(propertyData));
        }
        // nested entity property
        else {

            Object o = createObject(targetEntityMapping.getDescriptor(), propertyData);

            // Decode each of the property's children, setting values in the newly
            // created object.
            for (Element child : XMLUtil.getChildren(propertyData)) {
                decodeProperty(o, targetEntityMapping.getDescriptor(), child);
            }

            setProperty(object, propertyName, o);
        }
    }

    /**
     * Sets decoded object property. If a property is of Collection type, an object is
     * added to the collection.
     */
    private void setProperty(Object object, String propertyName, Object value) {

        // attempt to first set as a simple property, on failure try collection...
        // checking for collection first via 'PropertyUtils.getProperty' would throw an
        // exception on valid simple properties that are settable but not gettable

        try {
            PropertyUtils.setProperty(object, propertyName, value);
        }
        catch (CayenneRuntimeException e) {
            Object existingValue = PropertyUtils.getProperty(object, propertyName);
            if (existingValue instanceof Collection && !(value instanceof Collection)) {
                ((Collection) existingValue).add(value);
            }
            else {
                throw e;
            }
        }
    }

    /**
     * Instantiates a new object using information from entity mapping. Initializes all
     * properties that exist as 'objectData' attributes. Wraps all exceptions in
     * CayenneRuntimeException.
     * 
     * @param entityMapping Element that describes object to XML mapping.
     * @return The newly created object.
     * @throws CayenneRuntimeException
     */
    private Object createObject(Element entityMapping, Element objectData) {
        String className = entityMapping.getAttribute("name");

        Object object;
        try {
            object = Class.forName(
                    className,
                    true,
                    Thread.currentThread().getContextClassLoader()).newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating instance of class "
                    + className, ex);
        }
        
        // If a data context has been supplied by the user, then register the data object with the context.
        if ((null != objectContext) && (object instanceof Persistent)) {
            objectContext.registerNewObject(object);
        }

        NamedNodeMap attributes = objectData.getAttributes();
        for (int i = 0; i < attributes.getLength(); i++) {
            Attr attribute = (Attr) attributes.item(i);
            String propertyName = getPropertyMappingName(entityMapping, attribute.getName());

            if (propertyName != null) {
                PropertyUtils.setProperty(object, propertyName, attribute.getValue());
            }
        }

        return object;
    }
}
