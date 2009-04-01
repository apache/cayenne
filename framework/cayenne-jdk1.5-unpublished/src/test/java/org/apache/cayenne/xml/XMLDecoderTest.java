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

import java.io.InputStreamReader;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.unit.CayenneResources;

/**
 */
public class XMLDecoderTest extends TestCase {

    static final String XML_DATA_DIR = "xmlcoding/";
    protected XMLDecoder decoder;

    @Override
    public void setUp() {
        decoder = new XMLDecoder();
    }

    public void testDecode() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "encoded-object.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }

    public void testDecodeMappingAttributes() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "attribute-mapped.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
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
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "simple-mapped.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject("George", 57, false);
        assertEquals(decoded, george);
    }
    
    //  Added test for 1-to-1 relationship mappings, per CAY-597.
    public void testDecodeMapping1To1() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "1to1-mapped.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "1to1-mapping.xml").toExternalForm());
        
        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject grandParent = new TestObject();
        grandParent.setAge(117);
        grandParent.setName("Sue");
        grandParent.setOpen(false);
        
        TestObject parent = new TestObject();
        parent.setAge(94);
        parent.setName("Bill");
        parent.setOpen(true);
        parent.setParent(grandParent);
        
        TestObject child = new TestObject();
        child.setAge(57);
        child.setName("George");
        child.setOpen(false);
        child.setParent(parent);
        
        assertEquals(decoded, child);
    }

    public void testDecodeMappingCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "collection-mapped.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setAge(76);
        george.setName("George");
        assertEquals(decoded, george);

        List<TestObject> children = decoded.getChildren();
        assertEquals(children.size(), 2);

        TestObject bill = new TestObject();
        bill.setAge(34);
        bill.setName("Bill");
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject();
        sue.setAge(31);
        sue.setName("Sue");
        assertEquals(children.get(1), sue);

        List<TestObject> grandchildren = children.get(1).getChildren();
        assertEquals(grandchildren.size(), 1);

        TestObject mike = new TestObject();
        mike.setName("Mike");
        mike.setAge(3);
        assertEquals(grandchildren.get(0), mike);
    }

    public void testDecodeMappingCollectionWithNoEntity() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "collection-no-entity-mapped.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "collection-no-entity-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List<TestObject> children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(children.size(), 2);

        assertEquals("Bill", children.get(0));
        assertEquals("Sue", children.get(1));
    }

    public void testDecodeMappingCollection1() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "collection-mapped1.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List<TestObject> children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(children.size(), 2);

        assertEquals(children.get(0), new TestObject());
        assertEquals(children.get(1), new TestObject());
    }

    public void testDecodeMappingCollection2() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "collection-mapped2.xml"));
        Object object = decoder.decode(xml, CayenneResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");
        assertEquals(decoded, george);

        List<TestObject> children = decoded.getChildren();
        assertNotNull(children);
        assertEquals(1, children.size());

        // testing single empty child in collection
        assertEquals(children.get(0), new TestObject());
    }

    public void testDecodeCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "encoded-simple-collection.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        List<TestObject> children = decoded.getChildren();
        assertEquals(children.size(), 2);

        TestObject bill = new TestObject("Bill", 98, true);
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject("Sue", 45, false);
        assertEquals(children.get(1), sue);
    }

    public void testDecodeComplexCollection() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "encoded-complex-collection.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject george = new TestObject();
        george.setName("George");

        assertEquals(decoded, george);

        List<TestObject> children = decoded.getChildren();
        assertEquals(children.size(), 3);

        TestObject bill = new TestObject("Bill", 62, true);
        assertEquals(children.get(0), bill);

        TestObject sue = new TestObject("Sue", 8, true);
        assertEquals(children.get(1), sue);

        TestObject joe = new TestObject("Joe", 31, false);
        assertEquals(children.get(2), joe);

        List<TestObject> grandchildren = children.get(2).getChildren();
        assertEquals(grandchildren.size(), 1);

        TestObject harry = new TestObject("Harry", 23, false);
        assertEquals(grandchildren.get(0), harry);
    }

    public void testDecodePrimitives() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "encoded-object-primitives.xml"));
        Object object = decoder.decode(xml);

        assertTrue(object instanceof TestObject);
        TestObject test = (TestObject) object;
        assertEquals("n1", test.getName());
        assertEquals(5, test.getAge());
        assertEquals(true, test.isOpen());
    }
    
    //  Added test for 1-to-1 relationships, per CAY-597.
    public void testDecode1To1() throws Exception {
        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "1to1-encoded.xml"));
        Object object = decoder.decode(xml);
        
        assertTrue(object instanceof TestObject);
        TestObject decoded = (TestObject) object;

        TestObject grandParent = new TestObject();
        grandParent.setAge(117);
        grandParent.setName("Sue");
        grandParent.setOpen(false);
        
        TestObject parent = new TestObject();
        parent.setAge(94);
        parent.setName("Bill");
        parent.setOpen(true);
        parent.setParent(grandParent);
        
        TestObject child = new TestObject();
        child.setAge(57);
        child.setName("George");
        child.setOpen(false);
        child.setParent(parent);
        
        assertEquals(decoded, child);
    }

    public void testDecodeDataObjectsList() throws Exception {
        final List<TestObject> dataObjects = new ArrayList<TestObject>();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "data-objects-encoded.xml"));
        final List<?> decoded = XMLDecoder.decodeList(xml);

        assertEquals(dataObjects, decoded);
    }

    public void testDataObjectsListMapping() throws Exception {
        final List<TestObject> dataObjects = new ArrayList<TestObject>();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        Reader xml = new InputStreamReader(CayenneResources.getResource(XML_DATA_DIR
                + "data-objects-mapped.xml"));
        final List<?> decoded = XMLDecoder.decodeList(xml, CayenneResources
                .getResourceURL(XML_DATA_DIR + "simple-mapping.xml")
                .toExternalForm());

        assertEquals(dataObjects, decoded);
    }
}
