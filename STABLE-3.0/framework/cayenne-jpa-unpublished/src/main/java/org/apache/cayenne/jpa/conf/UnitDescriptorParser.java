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
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import javax.xml.XMLConstants;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.stream.StreamSource;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.cayenne.jpa.JpaUnit;
import org.apache.cayenne.jpa.Provider;
import org.apache.cayenne.jpa.instrument.InstrumentingUnit;
import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A parser of persistence descriptor files. Can be used in serial processing of multiple
 * documents.
 * 
 */
public class UnitDescriptorParser {

    static final String PERSISTENCE_SCHEMA = "META-INF/schemas/persistence_1_0.xsd";

    static final String PERSISTENCE = "persistence";
    static final String PERSISTENCE_UNIT = "persistence-unit";
    static final String DESCRIPTION = "description";
    static final String NAME = "name";
    static final String PROVIDER = "provider";
    static final String TRANSACTION_TYPE = "transaction-type";
    static final String JTA_DATASOURCE = "jta-data-source";
    static final String NON_JTA_DATASOURCE = "non-jta-data-source";
    static final String MAPPING_FILE = "mapping-file";
    static final String JAR_FILE = "jar-file";
    static final String CLASS = "class";
    static final String EXCLUDE_UNLISTED_CLASSES = "exclude-unlisted-classes";
    static final String PROPERTIES = "properties";
    static final String PROPERTY = "property";
    static final String VALUE = "value";

    protected SAXParserFactory parserFactory;

    public UnitDescriptorParser() throws SAXException, ParserConfigurationException {
        this(false);
    }

    public UnitDescriptorParser(boolean validatesAgainstSchema) throws SAXException {

        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);

        // note that validation requires that persistence.xml declares a schema like this:
        // <persistence xmlns="http://java.sun.com/xml/ns/persistence"
        // xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
        // xsi:schemaLocation="http://java.sun.com/xml/ns/persistence
        // http://java.sun.com/xml/ns/persistence/persistence_1_0.xsd">

        if (validatesAgainstSchema) {
            URL schemaURL = Thread.currentThread().getContextClassLoader().getResource(
                    PERSISTENCE_SCHEMA);

            SchemaFactory factory = SchemaFactory
                    .newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
            StreamSource ss = new StreamSource(schemaURL.toExternalForm());
            Schema schema = factory.newSchema(ss);
            parserFactory.setSchema(schema);
        }
    }

    /**
     * Loads and returns a Collection of PersistenceUnitInfos from the XML descriptor.
     */
    public Collection<JpaUnit> getPersistenceUnits(
            InputSource in,
            final URL persistenceUnitRootUrl) throws SAXException, IOException,
            ParserConfigurationException {

        final Collection<JpaUnit> unitInfos = new ArrayList<JpaUnit>(2);

        // note that parser is not reused - some parser implementations blow on
        // parser.reset() call
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(in, new DefaultHandler() {

            JpaUnit unit;
            Properties properties;
            StringBuilder charBuffer;

            @Override
            public void error(SAXParseException e) throws SAXException {
                throw e;
            }

            @Override
            public void startElement(
                    String uri,
                    String localName,
                    String qName,
                    Attributes attributes) throws SAXException {

                if (PERSISTENCE_UNIT.equals(qName)) {
                    String name = attributes.getValue("", NAME);
                    String transactionType = attributes.getValue("", TRANSACTION_TYPE);

                    unit = new InstrumentingUnit();
                    unit.setPersistenceUnitName(name);
                    unit.setPersistenceUnitRootUrl(persistenceUnitRootUrl);

                    if (transactionType != null) {
                        unit.putProperty(
                                Provider.TRANSACTION_TYPE_PROPERTY,
                                transactionType);
                    }
                }
                else if (PROPERTIES.equals(qName)) {
                    properties = new Properties();
                }
                else if (PROPERTY.equals(qName)) {
                    String name = attributes.getValue("", NAME);
                    String value = attributes.getValue("", VALUE);
                    properties.put(name, value);
                }
                else if (EXCLUDE_UNLISTED_CLASSES.equals(qName)) {
                    unit.setExcludeUnlistedClasses(true);
                }
            }

            @Override
            public void endElement(String uri, String localName, String qName)
                    throws SAXException {
                if (PERSISTENCE_UNIT.equals(qName)) {
                    unitInfos.add(unit);
                }
                else if (PROPERTIES.equals(qName)) {
                    unit.addProperties(properties);
                }
                else {
                    // process string values
                    String string = resetCharBuffer();

                    if (string != null) {
                        if (CLASS.equals(qName)) {
                            unit.addManagedClassName(string);
                        }
                        else if (PROVIDER.equals(qName)) {
                            unit.putProperty(Provider.PROVIDER_PROPERTY, string);
                        }
                        else if (JAR_FILE.equals(qName)) {
                            unit.addJarFileUrl(string);
                        }
                        else if (MAPPING_FILE.equals(qName)) {
                            unit.addMappingFileName(string);
                        }
                        else if (JTA_DATASOURCE.equals(qName)) {
                            unit.putProperty(Provider.JTA_DATA_SOURCE_PROPERTY, string);
                        }
                        else if (NON_JTA_DATASOURCE.equals(qName)) {
                            unit.putProperty(
                                    Provider.NON_JTA_DATA_SOURCE_PROPERTY,
                                    string);
                        }
                        else if (DESCRIPTION.equals(qName)) {
                            unit.setDescription(string);
                        }
                    }
                }
            }

            @Override
            public void characters(char[] ch, int start, int length) throws SAXException {
                if (charBuffer == null) {
                    charBuffer = new StringBuilder();
                }

                charBuffer.append(ch, start, length);
            }

            String resetCharBuffer() {
                if (charBuffer == null) {
                    return null;
                }

                String string = charBuffer.toString().trim();
                if (string.length() == 0) {
                    string = null;
                }
                charBuffer = null;

                return string;
            }

        });
        return unitInfos;
    }

}
