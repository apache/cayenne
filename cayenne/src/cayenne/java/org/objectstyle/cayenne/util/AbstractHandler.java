/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.util;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;
import org.xml.sax.XMLReader;
import org.xml.sax.helpers.DefaultHandler;


/**
 * The common superclass for all SAX event handlers used to parse
 * the configuration file. Each method just throws an exception, 
 * so subclasses should override what they can handle.
 *
 * Each type of XML element (map, node, etc.) has
 * a specific subclass.
 *
 * In the constructor, this class takes over the handling of SAX
 * events from the parent handler and returns
 * control back to the parent in the endElement method.
 * <p>
 * The idea to use nested handlers for XML document parsing
 * (and code to implement it) were taken from org.apache.tools.ant.ProjectHelper 
 * from Jakarta-Ant project (Copyright: Apache Software Foundation).
 * This may not be the best way to build objects from XML, but it is rather 
 * consistent. For each nested element in the XML tree a dedicated handler
 * is created (subclass of this AbstractHandler). Once the element is parsed, 
 * control is handled back to the parent handler.
 * </p>
 *
 * @author Andrei Adamchik 
 */
public class AbstractHandler extends DefaultHandler {
    /** Current parser. */
    protected XMLReader parser;

    /** Previous handler for the document.
     * When the next element is finished, control returns
     * to this handler. */
    protected ContentHandler parentHandler;

    /**
     * Creates a handler and sets the parser to use it
     * for the current element.
     * 
     * @param parser  Currently used XML parser. 
     *                Must not be <code>null</code>.
     * @param parentHandler The handler which should be restored to the 
     *                      parser at the end of the element. 
     *                      Must not be <code>null</code>.
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
     * Handles the start of an element. This base implementation just
     * throws an exception.
     * 
     * @exception SAXException if this method is not overridden, or in
     *                              case of error in an overridden version
     */
    public void startElement(String namespaceURI, String localName, String qName, Attributes atts)
    throws SAXException {
        throw new SAXException(this.getClass().getName() + ": unexpected element \"" + localName + "\"");
    }

    /**
     * Handles text within an element. This base implementation just
     * throws an exception.
     * 
     * @param buf A character array of the text within the element.
     *            Will not be <code>null</code>.
     * @param start The start element in the array.
     * @param count The number of characters to read from the array.
     * 
     * @exception SAXException if this method is not overridden, or in
     *                              case of error in an overridden version
     */
    public void characters(char[] buf, int start, int count) throws SAXException {
        String s = new String(buf, start, count).trim();

        if (s.length() > 0) {
            throw new SAXException(this.getClass().getName() + ": unexpected text \"" + s + "\"");
        }
    }

    /**
     * Called when this element and all elements nested into it have been
     * handled.
     */
    protected void finished() {}


    /**
     * Handles the end of an element. Any required clean-up is performed
     * by the finished() method and then the original handler is restored to
     * the parser.
     * 
     * @see #finished()
     */
    public void endElement(String namespaceURI, String localName, String qName)
    throws SAXException {
        finished();
        // Let parent resume handling SAX events
        parser.setContentHandler(parentHandler);
    }
}

