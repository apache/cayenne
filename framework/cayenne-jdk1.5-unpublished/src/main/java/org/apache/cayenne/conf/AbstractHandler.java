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

package org.apache.cayenne.conf;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;

/**
 * The common superclass for all SAX event handlers used to parse the configuration file.
 * Each method just throws an exception, so subclasses should override what they can
 * handle. Each type of XML element (map, node, etc.) has a specific subclass. In the
 * constructor, this class takes over the handling of SAX events from the parent handler
 * and returns control back to the parent in the endElement method.
 * </p>
 * 
 * @since 1.2
 */
class AbstractHandler extends DefaultHandler {

    /** Current parser. */
    protected XMLReader parser;

    /**
     * Previous handler for the document. When the next element is finished, control
     * returns to this handler.
     */
    protected ContentHandler parentHandler;

    /**
     * Creates a handler and sets the parser to use it for the current element.
     * 
     * @param parser Currently used XML parser. Must not be <code>null</code>.
     * @param parentHandler The handler which should be restored to the parser at the end
     *            of the element. Must not be <code>null</code>.
     */
    public AbstractHandler(XMLReader parser, ContentHandler parentHandler) {
        this.parentHandler = parentHandler;
        this.parser = parser;

        // Start handling SAX events
        parser.setContentHandler(this);
    }

    /** Returns currently used XMLReader. */
    public XMLReader getParser() {
        return parser;
    }

    /**
     * Handles the start of an element. This base implementation just throws an exception.
     * 
     * @exception SAXException if this method is not overridden, or in case of error in an
     *                overridden version
     */
    @Override
    public void startElement(
            String namespaceURI,
            String localName,
            String qName,
            Attributes atts) throws SAXException {
        throw new SAXException(this.getClass().getName()
                + ": unexpected element \""
                + localName
                + "\"");
    }

    /**
     * Handles text within an element. This base implementation just throws an exception.
     * 
     * @param buf A character array of the text within the element. Will not be
     *            <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * @exception SAXException if this method is not overridden, or in case of error in an
     *                overridden version
     */
    @Override
    public void characters(char[] buf, int start, int count) throws SAXException {
        String s = new String(buf, start, count).trim();

        if (s.length() > 0) {
            throw new SAXException(this.getClass().getName()
                    + ": unexpected text \""
                    + s
                    + "\"");
        }
    }

    /**
     * Called when this element and all elements nested into it have been handled.
     */
    protected void finished() {
    }

    /**
     * Handles the end of an element. Any required clean-up is performed by the finished()
     * method and then the original handler is restored to the parser.
     * 
     * @see #finished()
     */
    @Override
    public void endElement(String namespaceURI, String localName, String qName)
            throws SAXException {
        finished();
        // Let parent resume handling SAX events
        parser.setContentHandler(parentHandler);
    }
}
