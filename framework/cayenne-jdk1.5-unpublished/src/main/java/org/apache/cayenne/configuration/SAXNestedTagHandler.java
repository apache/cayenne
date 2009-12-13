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
package org.apache.cayenne.configuration;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A superclass of nested tag handlers for parsing of XML documents with SAX.
 * 
 * @since 3.1
 */
public class SAXNestedTagHandler extends DefaultHandler {

    private final static Locator NOOP_LOCATOR = new Locator() {

        public int getColumnNumber() {
            return -1;
        }

        public int getLineNumber() {
            return -1;
        }

        public String getPublicId() {
            return "<unknown>";
        }

        public String getSystemId() {
            return "<unknown>";
        }
    };

    protected XMLReader parser;
    protected ContentHandler parentHandler;
    protected Locator locator;

    public SAXNestedTagHandler(XMLReader parser, SAXNestedTagHandler parentHandler) {
        this.parentHandler = parentHandler;
        this.parser = parser;

        if (parentHandler != null) {
            locator = parentHandler.locator;
        }

        if (locator == null) {
            locator = NOOP_LOCATOR;
        }
    }

    protected String unexpectedTagMessage(String tagFound, String... tagsExpected) {

        List<String> expected = tagsExpected != null
                ? Arrays.asList(tagsExpected)
                : Collections.<String> emptyList();

        return String
                .format(
                        "tag <%s> is unexpected at [%d,%d]. The following tags are allowed here: %s",
                        tagFound,
                        locator.getColumnNumber(),
                        locator.getLineNumber(),
                        expected);
    }

    protected ContentHandler createChildTagHandler(
            String namespaceURI,
            String localName,
            String qName,
            Attributes attributes) {

        // loose handling of unrecognized tags - just ignore them
        return new SAXNestedTagHandler(parser, this);
    }

    protected void stop() {
        // pop self from the handler stack
        parser.setContentHandler(parentHandler);
    }

    @Override
    public final void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes attributes) throws SAXException {

        // push child handler to the stack...
        ContentHandler childHandler = createChildTagHandler(
                namespaceURI,
                localName,
                qName,
                attributes);
        parser.setContentHandler(childHandler);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        stop();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }
}
