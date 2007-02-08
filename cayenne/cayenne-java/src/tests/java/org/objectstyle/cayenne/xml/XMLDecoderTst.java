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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * @author Andrei Adamchik
 */
public class XMLDecoderTst extends TestCase {

    static final String XML_DATA_DIR = "xmlcoding/";
    protected XMLDecoder decoder;

    public void setUp() {
        decoder = new XMLDecoder();
    }

    public void testDecode() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "encoded-object.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }

    public void testDecodeMappingAttributes() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "attribute-mapped.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "attribute-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject("George", 57, true);
        assertEquals(george, decoded);
    }

    /**
     * Test decoding with a mapping file.
     * 
     * @throws Exception
     */
    public void testDecodeMapping() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "simple-mapped.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject("George", 57, false);
        assertEquals(decoded, george);
    }

    public void testDecodeMappingCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "collection-mapped.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List children = decoded.getChildren();
        assertEquals(children.size(), 2);

        TestObject bill = new TestObject();
        bill.setName("Bill");
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject();
        sue.setName("Sue");
        assertEquals(children.get(1), sue);

        List grandchildren = ((TestObject) children.get(1)).getChildren();
        assertEquals(grandchildren.size(), 1);

        TestObject mike = new TestObject();
        mike.setName("Mike");
        mike.setAge(3);
        assertEquals(grandchildren.get(0), mike);
    }

    public void testDecodeMappingCollectionWithNoEntity() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "collection-no-entity-mapped.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "collection-no-entity-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(children.size(), 2);

        assertEquals("Bill", children.get(0));
        assertEquals("Sue", children.get(1));
    }

    public void testDecodeMappingCollection1() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "collection-mapped1.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(children.size(), 2);

        assertEquals(children.get(0), new TestObject());
        assertEquals(children.get(1), new TestObject());
    }

    public void testDecodeMappingCollection2() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "collection-mapped2.xml"));
        Object object = decoder.decode(xml, CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());

        // testing single empty child in collection
        assertEquals(children.get(0), new TestObject());
    }

    public void testDecodeCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "encoded-simple-collection.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        List children = decoded.getChildren();
        assertEquals(children.size(), 2);

        TestObject bill = new TestObject("Bill", 98, true);
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject("Sue", 45, false);
        assertEquals(children.get(1), sue);
    }

    public void testDecodeComplexCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "encoded-complex-collection.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");

        assertEquals(decoded, george);

        List children = decoded.getChildren();
        assertEquals(children.size(), 3);

        TestObject bill = new TestObject("Bill", 62, true);
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject("Sue", 8, true);
        assertEquals(children.get(1), sue);

        TestObject joe = new TestObject("Joe", 31, false);
        assertEquals(children.get(2), joe);

        List grandchildren = ((TestObject) children.get(2)).getChildren();
        assertEquals(grandchildren.size(), 1);

        TestObject harry = new TestObject("Harry", 23, false);
        assertEquals(grandchildren.get(0), harry);
    }

    public void testDecodePrimitives() throws Exception {
        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "encoded-object-primitives.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }

    public void testDecodeDataObjectsList() throws Exception {
        final List dataObjects = new ArrayList();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "data-objects-encoded.xml"));
        final List decoded = XMLDecoder.decodeList(xml);

        assertEquals(dataObjects, decoded);
    }

    public void testDataObjectsListMapping() throws Exception {
        final List dataObjects = new ArrayList();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        Reader xml = new InputStreamReader(CayenneTestResources.getResource(XML_DATA_DIR
                + "data-objects-mapped.xml"));
        final List decoded = XMLDecoder.decodeList(xml, CayenneTestResources
                .getResourceURL(XML_DATA_DIR + "simple-mapping.xml")
                .toExternalForm());

        assertEquals(dataObjects, decoded);
    }
}