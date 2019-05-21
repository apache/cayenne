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
package org.apache.cayenne.configuration.xml;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

/**
 * A superclass of nested tag handlers for parsing of XML documents with SAX.
 * This class is not namespace aware, i.e. tags like &lt;info:property/> and &lt;property/>
 * will be treated as equal.
 * Use {@link NamespaceAwareNestedTagHandler} if you need to process namespaces.
 *
 * @see NamespaceAwareNestedTagHandler
 * @since 3.1
 * @since 4.1 redesigned and moved from {@link org.apache.cayenne.configuration} package
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

    protected LoaderContext loaderContext;
    protected ContentHandler parentHandler;
    protected Locator locator;

    public SAXNestedTagHandler(LoaderContext loaderContext) {
        this.loaderContext = Objects.requireNonNull(loaderContext);
        this.locator = NOOP_LOCATOR;
    }

    public SAXNestedTagHandler(SAXNestedTagHandler parentHandler) {
        this.parentHandler = Objects.requireNonNull(parentHandler);
        this.loaderContext = Objects.requireNonNull(parentHandler.loaderContext);

        locator = parentHandler.locator;
        if (locator == null) {
            locator = NOOP_LOCATOR;
        }
    }

    protected String unexpectedTagMessage(String tagFound, String... tagsExpected) {

        List<String> expected = tagsExpected != null
                ? Arrays.asList(tagsExpected)
                : Collections.emptyList();

        return String.format("tag <%s> is unexpected at [%d,%d]. The following tags are allowed here: %s",
                tagFound,
                locator.getColumnNumber(),
                locator.getLineNumber(),
                expected);
    }

    /**
     * Main method to process XML content.
     * Should be override in subclasses, by default do nothing.
     * Return value should be true if tag was fully processed and shouldn't be passed down to child handler.
     *
     * @param namespaceURI namespace for tag
     * @param localName tag local name (i.e. w/o namespace prefix)
     * @param attributes tag attributes
     *
     * @return true if tag was processed
     *
     * @throws SAXException can be thrown to abort parsing
     *
     * @see #createChildTagHandler(String, String, String, Attributes)
     */
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return true;
    }

    /**
     * Callback method that is called before this handler pushed out of parsers stack.
     * Can be used to flush some aggregate state.
     */
    protected void beforeScopeEnd() {
    }

    /**
     * This method should be used to create nested handlers to process children elements.
     * This method should never return {@code null}.
     *
     * @param namespaceURI namespace for tag
     * @param localName tag local name (i.e. w/o namespace prefix)
     * @param qName tag full name (i.e. with namespace prefix)
     * @param attributes tag attributes
     * @return new handler to process child tag
     */
    protected ContentHandler createChildTagHandler(String namespaceURI, String localName,
                                                   String qName, Attributes attributes) {
        // loose handling of unrecognized tags - just ignore them
        return new SAXNestedTagHandler(this);
    }

    protected void stop() {
        beforeScopeEnd();
        // pop self from the handler stack
        loaderContext.getXmlReader().setContentHandler(parentHandler);
    }

    /**
     * This method directly called by SAX parser, do not override it directly,
     * use {@link #processElement(String, String, Attributes)} method instead to process content.
     *
     * @see #createChildTagHandler(String, String, String, Attributes)
     */
    @Override
    public void startElement(String namespaceURI, String localName,
                             String qName, Attributes attributes) throws SAXException {
        ContentHandler childHandler = createChildTagHandler(namespaceURI, localName, qName, attributes);

        if(!processElement(namespaceURI, localName, attributes)) {
            childHandler.startElement(namespaceURI, localName, qName, attributes);
        }

        // push child handler to the stack...
        loaderContext.getXmlReader().setContentHandler(childHandler);
    }

    @Override
    public void endElement(String namespaceURI, String localName, String qName) throws SAXException {
        stop();
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        this.locator = locator;
    }

    public ContentHandler getParentHandler() {
        return parentHandler;
    }
}
