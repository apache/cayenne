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
package org.apache.cayenne.instrument;

import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;

import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * Extracts a list of JPA unit names available in the environment.
 * 
 * @since 3.0
 */
// some code duplication with JPA UnitLoader and UnitDescriptorParser, but here we only
// care about persistence unit names, and not the full contents.
class JpaUnitNameParser {

    static final String DESCRIPTOR_LOCATION = "META-INF/persistence.xml";
    static final String PERSISTENCE_UNIT = "persistence-unit";
    static final String NAME = "name";

    private SAXParserFactory parserFactory;

    public JpaUnitNameParser() {
        parserFactory = SAXParserFactory.newInstance();
        parserFactory.setNamespaceAware(true);
    }

    Collection<String> getUnitNames() {

        Collection<String> unitNames = new ArrayList<String>(5);

        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            Enumeration<URL> descriptors = loader.getResources(DESCRIPTOR_LOCATION);
            while (descriptors.hasMoreElements()) {
                String descriptorURL = descriptors.nextElement().toExternalForm();
                appendUnitNames(unitNames, new InputSource(descriptorURL));
            }
        }
        catch (Exception e) {
            throw new RuntimeException("Error inspecting persistence descriptors", e);
        }

        return unitNames;
    }

    private void appendUnitNames(final Collection<String> unitNames, InputSource in)
            throws Exception {

        // note that parser is not reused - some parser implementations blow on
        // parser.reset() call
        SAXParser parser = parserFactory.newSAXParser();
        parser.parse(in, new DefaultHandler() {

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
                    if (name != null) {
                        unitNames.add(name);
                    }
                }
            }
        });
    }
}
