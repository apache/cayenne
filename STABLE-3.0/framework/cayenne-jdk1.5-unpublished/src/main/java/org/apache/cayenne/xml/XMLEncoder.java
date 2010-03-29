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

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Result;
import javax.xml.transform.Source;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.apache.cayenne.CayenneRuntimeException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * A helper class to encode objects to XML.
 * 
 * @since 1.2
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
    protected void initDocument(String rootTag, String type) {
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
    protected Node getRootNode(boolean forceSyntheticRoot) {
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

    protected String nodeToString(Node rootNode) {

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

    protected void setRoot(String xmlTag, String type, boolean push) {

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

    protected void encodeProperty(String xmlTag, Object value, boolean useType) {
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
            encodeCollection(xmlTag, (Collection<Object>) value, useType);
        }
        else {
            encodeSimple(xmlTag, value, useType);
        }
    }

    protected void encodeSimple(String xmlTag, Object object, boolean useType) {

        // simple properties will not call setRoot, so push manually
        Element node = push(xmlTag);

        if (useType) {
            node.setAttribute("type", object.getClass().getName());
        }

        // Dates need special handling
        if (object instanceof Date) {
            SimpleDateFormat sdf = new SimpleDateFormat(XMLUtil.DEFAULT_DATE_FORMAT);
            node.appendChild(document.createTextNode(sdf.format(object)));
        }
        else {
            node.appendChild(document.createTextNode(object.toString()));
        }

        pop();
    }

    protected void encodeSerializable(String xmlTag, XMLSerializable object) {
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
    protected void encodeCollection(String xmlTag, Collection<Object> c, boolean useType) {
        // encode collection without doing push/pop so that its elements are encoded
        // without an intermediate grouping node.
        for (Object o : c) {
            encodeProperty(xmlTag, o, useType);
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
