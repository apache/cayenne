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

import java.io.Reader;
import java.lang.reflect.Constructor;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.reflect.PropertyUtils;
import org.apache.cayenne.util.Util;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.InputSource;

/**
 * XMLDecoder is used to decode XML into objects.
 * 
 * @since 1.2
 */
public class XMLDecoder {

    static final Map<String, Class<?>> classMapping = new HashMap<String, Class<?>>();

    static {
        classMapping.put("boolean", Boolean.class);
        classMapping.put("int", Integer.class);
        classMapping.put("char", Character.class);
        classMapping.put("float", Float.class);
        classMapping.put("byte", Byte.class);
        classMapping.put("short", Short.class);
        classMapping.put("long", Long.class);
        classMapping.put("double", Double.class);
    }

    /** The root of the XML document being decoded. */
    private Element root;

    /** The context to register decoded DataObjects with. */
    private ObjectContext objectContext;

    // TODO: H to the A to the C to the K
    private List<Element> decodedCollections = new ArrayList<Element>();

    /**
     * Default constructor. This will create an XMLDecoder instance that will decode
     * objects from XML, but will not register them with any context.
     * 
     * @see XMLDecoder#XMLDecoder(ObjectContext)
     */
    public XMLDecoder() {
        this(null);
    }

    /**
     * Creates an XMLDecoder that will register decoded DataObjects with the specified
     * context.
     * 
     * @param objectContext The context to register decoded DataObjects with.
     */
    public XMLDecoder(ObjectContext objectContext) {
        this.objectContext = objectContext;
    }

    /**
     * Decodes an XML element to a Boolean.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Boolean decodeBoolean(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Boolean.valueOf(val);
    }

    /**
     * Decodes an XML element to a Double.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Double decodeDouble(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Double.valueOf(val);
    }

    /**
     * Decodes an XML element to a Float.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Float decodeFloat(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Float.valueOf(val);
    }

    /**
     * Decodes an XML element to an Integer.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public Integer decodeInteger(String xmlTag) {
        String val = decodeString(xmlTag);

        if (null == val) {
            return null;
        }

        return Integer.valueOf(val);
    }

    /**
     * Decodes an object from XML.
     * 
     * @param xmlTag The XML tag corresponding to the root of the encoded object.
     * @return The decoded object.
     */
    public Object decodeObject(String xmlTag) {
        // Find the XML element corresponding to the supplied tag.
        Element child = XMLUtil.getChild(root, xmlTag);

        return decodeObject(child);
    }

    /**
     * Decodes an XML element to an Object.
     * 
     * @param child The XML element.
     * @return The tag's value.
     */
    protected Object decodeObject(Element child) {

        if (null == child) {
            return null;
        }

        String type = child.getAttribute("type");
        if (Util.isEmptyString(type)) {
            // TODO should we use String by default? Or guess from the property type?
            throw new CayenneRuntimeException("No type specified for tag '"
                    + child.getNodeName()
                    + "'.");
        }

        // temp hack to support primitives...
        Class<?> objectClass = classMapping.get(type);
        if (null == objectClass) {
            try {
                objectClass = Class.forName(type);
            }
            catch (Exception e) {
                throw new CayenneRuntimeException("Unrecognized class '"
                        + objectClass
                        + "'", e);
            }
        }

        try {
            // This crazy conditional checks if we're decoding a collection. There are two
            // ways to enter into this body:
            // 1) If there are two elements at the same level with the same name, then
            // they should be part of a collection.
            // 2) If a single occurring element has the "forceList" attribute set to
            // "YES", then it too should be treated as a collection.
            // 
            // The final part checks that we have not previously attempted to decode this
            // collection, which is necessary to prevent infinite loops .
            if ((((null != child.getParentNode()) && (XMLUtil.getChildren(
                    child.getParentNode(),
                    child.getNodeName()).size() > 1)) || (child
                    .getAttribute("forceList")
                    .toUpperCase().equals("YES")))
                    && (!decodedCollections.contains(child))) {
                return decodeCollection(child);
            }

            // If the object implements XMLSerializable, delegate decoding to the class's
            // implementation of decodeFromXML().
            if (XMLSerializable.class.isAssignableFrom(objectClass)) {
                // Fix for decoding 1-to-1 relationships between the same class type, per CAY-597.
                // If we don't re-root the tree, the decoder goes into an infinite loop.  In particular,
                // if R1 -> R2, when it decodes R1, it will attempt to decode R2, but without re-rooting,
                // the decoder tries to decode R1 again, think it's decoding R2, because R1 is the first
                // element of that type found in the XML doc with the true root of the doc.
                Element oldRoot = root;
                root = child;
                
                XMLSerializable ret = (XMLSerializable) objectClass.newInstance();
                ret.decodeFromXML(this);

                // Restore the root when we're done decoding the child.
                root = oldRoot;
                
                return ret;
            }
            
            String text = XMLUtil.getText(child);

            // handle dates using hardcoded format....
            if (Date.class.isAssignableFrom(objectClass)) {
                try {
                    return new SimpleDateFormat(XMLUtil.DEFAULT_DATE_FORMAT).parse(text);
                }
                catch (ParseException e) {
                    // handle pre-3.0 default data format for backwards compatibilty
                    
                    try {
                        return new SimpleDateFormat("E MMM dd hh:mm:ss z yyyy").parse(text);
                    }
                    catch (ParseException eOld) {
                        
                        // rethrow the original exception
                        throw e;
                    }
                }
            }

            // handle all other primitive types...
            Constructor<?> c = objectClass.getConstructor(String.class);
            return c.newInstance(text);
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error decoding tag '"
                    + child.getNodeName()
                    + "'", e);
        }
    }

    /**
     * Decodes an XML element to a String.
     * 
     * @param xmlTag The tag identifying the element.
     * @return The tag's value.
     */
    public String decodeString(String xmlTag) {
        // Find the XML element corresponding to the supplied tag, and simply
        // return its text.
        Element child = XMLUtil.getChild(root, xmlTag);
        return child != null ? XMLUtil.getText(child) : null;
    }

    /**
     * Decodes XML wrapped by a Reader into an object.
     * 
     * @param xml Wrapped XML.
     * @return A new instance of the object represented by the XML.
     * @throws CayenneRuntimeException
     */
    public Object decode(Reader xml) throws CayenneRuntimeException {

        // Parse the XML into a JDOM representation.
        Document data = parse(xml);

        // Delegate to the decode() method that works on JDOM elements.
        return decodeElement(data.getDocumentElement());
    }

    /**
     * Decodes XML wrapped by a Reader into an object, using the supplied mapping file to
     * guide the decoding process.
     * 
     * @param xml Wrapped XML.
     * @param mappingUrl Mapping file describing how the XML elements and object
     *            properties correlate.
     * @return A new instance of the object represented by the XML.
     * @throws CayenneRuntimeException
     */
    public Object decode(Reader xml, String mappingUrl) throws CayenneRuntimeException {
        // Parse the XML document into a JDOM representation.
        Document data = parse(xml);

        // MappingUtils will really do all the work.
        XMLMappingDescriptor mu = new XMLMappingDescriptor(mappingUrl);

        Object ret = mu.decode(data.getDocumentElement(), objectContext);

        return ret;
    }

    /**
     * Decodes the XML element to an object. If the supplied context is not null, the
     * object will be registered with it and committed to the database.
     * 
     * @param element The XML element.
     * @return The decoded object.
     * @throws CayenneRuntimeException
     */
    protected Object decodeElement(Element element) throws CayenneRuntimeException {

        // Update root to be the supplied xml element. This is necessary as
        // root is used for decoding properties.
        Element oldRoot = root;
        root = element;

        // Create the object we're ultimately returning. It is represented
        // by the root element of the XML.
        Object object;

        try {
            object = decodeObject(element);
        }
        catch (Throwable th) {
            throw new CayenneRuntimeException("Error instantiating object", th);
        }

        if ((null != objectContext) && (object instanceof Persistent)) {
            objectContext.registerNewObject(object);
        }

        root = oldRoot;
        decodedCollections.clear();

        return object;
    }

    /**
     * Decodes a Collection represented by XML wrapped by a Reader into a List of objects.
     * Each object will be registered with the supplied context.
     * 
     * @param xml The XML element representing the elements in the collection to decode.
     * @return A List of all the decoded objects.
     * @throws CayenneRuntimeException
     */
    protected Collection<Object> decodeCollection(Element xml) throws CayenneRuntimeException {

        Collection<Object> ret;
        try {
            String parentClass = ((Element) xml.getParentNode()).getAttribute("type");
            Object property = Class.forName(parentClass).newInstance();
            Collection<Object> c = (Collection<Object>) PropertyUtils.getProperty(property, xml
                    .getNodeName());

            ret = c.getClass().newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                    "Could not create collection with no-arg constructor.",
                    ex);
        }

        // Each child of the root corresponds to an XML representation of
        // the object. The idea is decode each of those into an object and add them to the
        // list to be returned.
        for (Element e : XMLUtil.getChildren(xml.getParentNode(), xml.getNodeName())) {
            // Decode the object.
            decodedCollections.add(e);
            Object o = decodeElement(e);

            // Add it to the output list.
            ret.add(o);
        }

        return ret;
    }

    /**
     * Decodes a list of DataObjects.
     * 
     * @param xml The wrapped XML encoding of the list of DataObjects.
     * @return The list of decoded DataObjects.
     * @throws CayenneRuntimeException
     */
    public static List<Object> decodeList(Reader xml) throws CayenneRuntimeException {
        return decodeList(xml, null, null);
    }

    /**
     * Decodes a list of DataObjects, registering them the supplied context.
     * 
     * @param xml The wrapped XML encoding of the list of DataObjects.
     * @param objectContext The context to register the decode DataObjects with.
     * @return The list of decoded DataObjects.
     * @throws CayenneRuntimeException
     */
    public static List<Object> decodeList(Reader xml, ObjectContext objectContext)
            throws CayenneRuntimeException {
        return decodeList(xml, null, objectContext);
    }

    /**
     * Decodes a list of DataObjects using the supplied mapping file to guide the decoding
     * process.
     * 
     * @param xml The wrapped XML encoding of the list of DataObjects.
     * @param mappingUrl Mapping file describing how the XML elements and object
     *            properties correlate.
     * @return The list of decoded DataObjects.
     * @throws CayenneRuntimeException
     */
    public static List<Object> decodeList(Reader xml, String mappingUrl)
            throws CayenneRuntimeException {
        return decodeList(xml, mappingUrl, null);
    }

    /**
     * Decodes a list of DataObjects using the supplied mapping file to guide the decoding
     * process, registering them the supplied context.
     * 
     * @param xml The wrapped XML encoding of the list of objects.
     * @param mappingUrl Mapping file describing how the XML elements and object
     *            properties correlate.
     * @param objectContext The context to register the decode DataObjects with.
     * @return The list of decoded DataObjects.
     * @throws CayenneRuntimeException
     */
    public static List<Object> decodeList(Reader xml, String mappingUrl, ObjectContext objectContext)
            throws CayenneRuntimeException {

        XMLDecoder decoder = new XMLDecoder(objectContext);
        Element listRoot = parse(xml).getDocumentElement();

        List<Object> ret;
        try {
            String parentClass = listRoot.getAttribute("type");
            ret = (List<Object>) Class.forName(parentClass).newInstance();
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException(
                    "Could not create collection with no-arg constructor.",
                    ex);
        }

        XMLMappingDescriptor mu = null;
        if (mappingUrl != null) {
            mu = new XMLMappingDescriptor(mappingUrl);
        }

        // Each child of the root corresponds to an XML representation of
        // the object. The idea is decode each of those into an object and add them to the
        // list to be returned.
        for (Element e : XMLUtil.getChildren(listRoot)) {
            // Decode the object.
            decoder.decodedCollections.add(e);

            // Decode the item using the appropriate decoding method.
            Object o;

            if (mu != null) {
                o = mu.decode(e, objectContext);
            }
            else {
                // The decoder will do context registration if needed.
                o = decoder.decodeElement(e);
            }

            ret.add(o);
        }

        return ret;
    }

    /**
     * Takes the XML wrapped in a Reader and returns a JDOM Document representation of it.
     * 
     * @param in Wrapped XML.
     * @return DOM Document wrapping the XML for use throughout the rest of the decoder.
     * @throws CayenneRuntimeException
     */
    private static Document parse(Reader in) throws CayenneRuntimeException {
        DocumentBuilder builder = XMLUtil.newBuilder();

        try {
            return builder.parse(new InputSource(in));
        }
        catch (Exception ex) {
            throw new CayenneRuntimeException("Error parsing XML", ex);
        }
    }
}
