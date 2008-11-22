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

package org.apache.cayenne.jpa.conf;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;

import javax.xml.XMLConstants;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cayenne.jpa.map.JpaEntityMap;
import org.apache.cayenne.xml.XMLDecoder;
import org.xml.sax.SAXException;

/**
 * {@link org.apache.cayenne.jpa.map.JpaEntityMap} loader that reads mapping information
 * from the XML sources compatible with the JPA ORM schema.
 * 
 */
public class EntityMapXMLLoader {

    static final String XML_CODER_MAPPING = "META-INF/cayenne/orm-coder.xml";
    static final String ORM_SCHEMA = "META-INF/schemas/orm_1_0.xsd";

    Schema schema;

    protected ClassLoader classLoader;

    public EntityMapXMLLoader() {
        this(Thread.currentThread().getContextClassLoader(), false);
    }

    public EntityMapXMLLoader(ClassLoader classLoader, boolean validateDescriptors) {

        this.classLoader = classLoader;

        // TODO: andrus, 04/18/2006 - merge validation capabilities to the XMLDecoder...
        if (validateDescriptors) {
            SAXParserFactory parserFactory = SAXParserFactory.newInstance();
            parserFactory.setNamespaceAware(true);

            // note that validation requires that orm.xml declares a schema like this:
            // <orm xmlns="http://java.sun.com/xml/ns/persistence/orm"
            // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
            // xsi:schemaLocation="http://java.sun.com/xml/ns/persistence/orm
            // http://java.sun.com/xml/ns/persistence/orm/orm_1_0.xsd">

            URL schemaURL = classLoader.getResource(ORM_SCHEMA);

            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            StreamSource ss = new StreamSource(schemaURL.toExternalForm());

            try {
                this.schema = factory.newSchema(ss);
            }
            catch (Exception e) {
                throw new RuntimeException("Error loading ORM schema", e);
            }
        }
    }

    /**
     * Loads {@link JpaEntityMap} using provided class loader to locate the XML. Returns
     * null if no mapping is found.
     */

    public JpaEntityMap getEntityMap(URL resource) {

        // TODO: andrus, 04/18/2006 XMLDecoder should support classpath locations for
        // mapping descriptors
        URL mappingURL = classLoader.getResource(XML_CODER_MAPPING);
        if (mappingURL == null) {
            throw new RuntimeException("No code mapping found: " + XML_CODER_MAPPING);
        }

        validate(resource);

        try {

            Reader in = new InputStreamReader(resource.openStream(), "UTF-8");

            // TODO: andrus, 04/18/2006 - an inefficiency in XMLDecoder - it
            // doesn't cache the mapping

            XMLDecoder decoder = new XMLDecoder();
            JpaEntityMap entityMap = (JpaEntityMap) decoder.decode(in, mappingURL
                    .toExternalForm());

            return entityMap;
        }
        catch (Exception e) {
            throw new RuntimeException("Error processing ORM mapping " + resource, e);
        }
    }

    // TODO: andrus, 04/18/2006 - move schema validation to the XMLDecoder
    void validate(URL resource) {
        if (schema != null) {
            try {
                schema.newValidator().validate(new StreamSource(resource.openStream()));
            }
            catch (SAXException e) {
                throw new RuntimeException("Error validating ORM mapping " + resource, e);
            }
            catch (IOException e) {
                throw new RuntimeException("Error processing ORM mapping " + resource, e);
            }
        }
    }
}
