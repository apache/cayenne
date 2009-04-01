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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.apache.cayenne.unit.CayenneResources;

/**
 */
public class XMLEncoderTest extends TestCase {

    static final String XML_DATA_DIR = "xmlcoding/";
    static final boolean windows;
    
    static {
        if (System.getProperty("os.name").toUpperCase().contains("WINDOWS")) {
            windows = true;
        }
        else {
            windows = false;
        }
    }

    public void testObjectWithNullProperties() throws Exception {
        XMLEncoder encoder = new XMLEncoder();

        TestObject test = new TestObject();
        test.setName(null);
        test.encodeAsXML(encoder);
    }

    public void testEncodeSimpleCollection() throws Exception {
        XMLEncoder encoder = new XMLEncoder();

        TestObject test = new TestObject();
        test.addChild(new TestObject("Bill", 98, true));
        test.addChild(new TestObject("Sue", 45, false));

        encoder.setRoot("Test", test.getClass().getName());
        encoder.encodeProperty("children", test.getChildren());
        String result = encoder.nodeToString(encoder.getRootNode(false));

        String comp = loadTestFileAsString("encoded-simple-collection.xml");

        assertEquals(comp, result);
    }

    private String loadTestFileAsString(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(CayenneResources
                .getResource(XML_DATA_DIR + filename)));
        StringBuffer comp = new StringBuffer();
        while (in.ready()) {
            comp.append(in.readLine());
            if (windows) {
                comp.append("\r");
            }
            comp.append("\n");
        }
        in.close();
        
        return comp.toString();
    }

    public void testEncodeComplexCollection() throws Exception {
        XMLEncoder encoder = new XMLEncoder();
        TestObject obj1 = new TestObject();
        obj1.setName("George");
        obj1.addChild(new TestObject("Bill", 62, true));
        obj1.addChild(new TestObject("Sue", 8, true));

        TestObject obj2 = new TestObject("Joe", 31, false);
        obj2.addChild(new TestObject("Harry", 23, false));

        obj1.addChild(obj2);

        String result = encoder.encode("TestObjects", obj1);

        String comp = loadTestFileAsString("encoded-complex-collection.xml");
        // there are differences in attribute order encoding, so there can be more than
        // one valid output depending on the parser used...

        if (!comp.equals(result)) {
            comp = loadTestFileAsString("encoded-complex-collection-alt1.xml");
        }
        assertEquals(comp, result);
    }

    public void testSimpleMapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm());
        TestObject test = new TestObject();
        test.setAge(57);
        test.setName("George");
        test.setOpen(false);

        String result = encoder.encode(test);

        String comp = loadTestFileAsString("simple-mapped.xml");
        
        assertEquals(comp, result);
    }
    
    //  Added test for 1-to-1 relationship mappings, per CAY-597.
    public void test1To1Mapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneResources.getResourceURL(
                XML_DATA_DIR + "1to1-mapping.xml").toExternalForm());
        
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
        
        String result = encoder.encode(child);

        String comp = loadTestFileAsString("1to1-mapped.xml");
        
        assertEquals(comp, result);
    }
    
    // Added test for 1-to-1 relationships, per CAY-597.
    public void testEncode1To1() throws Exception {
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
        
        String xml = new XMLEncoder().encode("Test", child);

        String comp = loadTestFileAsString("1to1-encoded.xml");
        
        assertEquals(comp, xml);
    }

    public void testCollectionMapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());
        TestObject george = new TestObject();
        george.setAge(76);
        george.setName("George");
        george.addChild(new TestObject("Bill", 34, true));

        TestObject sue = new TestObject("Sue", 31, false);
        sue.addChild(new TestObject("Mike", 3, true));
        george.addChild(sue);

        String result = encoder.encode(george);

        String comp = loadTestFileAsString("collection-mapped.xml");

        assertEquals(comp, result);
    }

    public void testEncodeDataObjectsList() throws Exception {
        List<TestObject> dataObjects = new ArrayList<TestObject>();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        String xml = new XMLEncoder().encode("EncodedTestList", dataObjects);

        String comp = loadTestFileAsString("data-objects-encoded.xml");
        
        assertEquals(comp, xml);
    }

    public void testDataObjectsListMapping() throws Exception {
        List<TestObject> dataObjects = new ArrayList<TestObject>();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        String xml = new XMLEncoder(CayenneResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm()).encode(
                "EncodedTestList",
                dataObjects);

        String comp = loadTestFileAsString("data-objects-mapped.xml");

        assertEquals(comp, xml);
    }
}
