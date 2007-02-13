/* ====================================================================
 *
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
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
package org.objectstyle.cayenne.xml;

import java.io.StringWriter;
import java.util.Collection;
import java.util.Iterator;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.2
 * @author Kevin J. Menard, Jr., Andrus Adamchik
 */
public class XMLEncoder {

    private XMLMappingDescriptor mappingDescriptor;

    // temp ivars that define the structure of the encoding stack.
    private Document document;
    private Node parent;
    private String tagOverride;
    private boolean inProgress;

    /**
     * Creates new XMLEncoder.
     */
    public XMLEncoder() {

    }

    /**
     * Creates new XMLEncoder that will use a mapping descriptor loaded via provided URL.
     */
    public XMLEncoder(String mappingUrl) {
        this.mappingDescriptor = new XMLMappingDescriptor(mappingUrl);
    }

    /**
     * A callback for XMLSerializable objects to add a node to an encoding tree.
     */
    public void setRoot(String xmlTag, String type) {
        setRoot(xmlTag, type, true);
    }

    /**
     * A callback method for XMLSerializable objects to encode an object property. Note
     * that the object must call "setRoot" prior to encoding its properties.
     * 
     * @param xmlTag The name of the XML element used to represent the property.
     * @param value The object's property value to encode.
     */
    public void encodeProperty(String xmlTag, Object value) {
        encodeProperty(xmlTag, value, true);
    }

    /**
     * Encodes an object using "root" as a root tag.
     */
    public String encode(Object object) throws CayenneRuntimeException {
        return encode("root", object);
    }

    /**
     * Encodes using provided root XML tag.
     */
    public String encode(String rootTag, Object object) throws CayenneRuntimeException {

        try {
            initDocument(rootTag, object != null ? object.getClass().getName() : null);

            if (mappingDescriptor != null) {
                mappingDescriptor.getRootEntity().setObject(object);
                mappingDescriptor.getRootEntity().encodeAsXML(this);
            }
            else {
                encodeProperty(rootTag, object);
            }

            if (object instanceof Collection) {
                return nodeToString(getRootNode(true));
            } else {
                return nodeToString(getRootNode(false));
            }
        }
        finally {
            // make sure we can restart the encoder...
            inProgress = false;
        }
    }

    /**
     * Resets the encoder to process a new object tree.
     */
    void initDocument(String rootTag, String type) {
        this.document = XMLUtil.newBuilder().newDocument();
        this.parent = document;

        // create a "synthetic" root
        Element root = push(rootTag);
        if (type != null) {
            root.setAttribute("type", type);
        }

        inProgress = true;
    }

    /**
     * Returns a root DOM node of the encoder.
     */
    Node getRootNode(boolean forceSyntheticRoot) {
        if (document == null) {
            return null;
        }

        // if synthetic root has a single child, use child as a root
        Node root = document.getDocumentElement();
        
        if (!forceSyntheticRoot && root.getChildNodes().getLength() == 1) {
            root = root.getFirstChild();
        }

        return root;
    }

    String nodeToString(Node rootNode) {

        StringWriter out = new StringWriter();
        Result result = new StreamResult(out);

        Source source = new DOMSource(rootNode);

        try {
            Transformer xformer = TransformerFactory.newInstance().newTransformer();
            xformer.setOutputProperty(OutputKeys.METHOD, "xml");
            xformer.setOutputProperty(OutputKeys.INDENT, "yes");
            xformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
            xformer.transform(source, result);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("XML transformation error", e);
        }

        return out.toString().trim() + System.getProperty("line.separator");
    }

    void setRoot(String xmlTag, String type, boolean push) {

        // all public methods must implicitly init the document
        if (!inProgress) {
            initDocument(xmlTag, type);
        }

        if (tagOverride != null) {
            xmlTag = tagOverride;
            tagOverride = null;
        }

        // new node will be a peer rather than a child of current parent.
        if (!push) {
            pop();
        }

        Element element = push(xmlTag);
        if (type != null) {
            element.setAttribute("type", type);
        }
    }

    void encodeProperty(String xmlTag, Object value, boolean useType) {
        // all public methods must be able to implicitly init the document
        if (!inProgress) {
            String type = (useType && value != null) ? value.getClass().getName() : null;
            initDocument(xmlTag, type);
        }

        if (value == null) {
            return;
        }
        else if (value instanceof XMLSerializable) {
            encodeSerializable(xmlTag, (XMLSerializable) value);
        }
        else if (value instanceof Collection) {
            encodeCollection(xmlTag, (Collection) value, useType);
        }
        else {
            encodeSimple(xmlTag, value, useType);
        }
    }

    void encodeSimple(String xmlTag, Object object, boolean useType) {

        // simple properties will not call setRoot, so push manually
        Element node = push(xmlTag);

        if (useType) {
            node.setAttribute("type", object.getClass().getName());
        }

        node.appendChild(document.createTextNode(object.toString()));
        pop();
    }

    void encodeSerializable(String xmlTag, XMLSerializable object) {
        // don't allow children to reset XML tag name ... unless they are at the root
        // level
        if (document.getDocumentElement() != parent) {
            tagOverride = xmlTag;
        }

        object.encodeAsXML(this);

        tagOverride = null;
        pop();
    }

    /**
     * Encodes a collection of objects, attaching them to the current root.
     * 
     * @param xmlTag The name of the root XML element for the encoded collection.
     * @param c The collection to encode.
     */
    void encodeCollection(String xmlTag, Collection c, boolean useType) {

        Iterator it = c.iterator();
        while (it.hasNext()) {
            // encode collection without doing push/pop so that its elements are encoded
            // without an intermediate grouping node.
            encodeProperty(xmlTag, it.next(), useType);
        }

        if (c.size() == 1) {
            ((Element) parent.getLastChild()).setAttribute("forceList", "YES");
        }
    }

    /**
     * Creates a new element, pushing it onto encoding stack.
     */
    private Element push(String xmlTag) {

        Element child = document.createElement(xmlTag);
        this.parent.appendChild(child);
        this.parent = child;
        return child;
    }

    /**
     * Pops the top element from the encoding stack.
     */
    Node pop() {
        Node old = parent;
        parent = parent.getParentNode();
        return old;
    }
}
