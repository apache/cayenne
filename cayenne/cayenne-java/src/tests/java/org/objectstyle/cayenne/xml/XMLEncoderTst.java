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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;

import org.objectstyle.cayenne.unit.CayenneTestResources;

/**
 * @author Kevin J. Menard, Jr.
 */
public class XMLEncoderTst extends TestCase {

    static final String XML_DATA_DIR = "xmlcoding/";
    static final boolean windows;
    
    static {
        if (System.getProperty("os.name").toUpperCase().indexOf("WINDOWS") >= 0) {
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

        assertEquals(comp.toString(), result);
    }

    private String loadTestFileAsString(String filename) throws IOException {
        BufferedReader in = new BufferedReader(new InputStreamReader(CayenneTestResources
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

        if (!comp.toString().equals(result)) {
            comp = loadTestFileAsString("encoded-complex-collection-alt1.xml");
        }
        assertEquals(comp.toString(), result);
    }

    public void testSimpleMapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm());
        TestObject test = new TestObject();
        test.setAge(57);
        test.setName("George");
        test.setOpen(false);

        String result = encoder.encode(test);

        String comp = loadTestFileAsString("simple-mapped.xml");
        
        assertEquals(comp.toString(), result);
    }
    
    // Added test for 1-to-1 relationship mappings, per CAY-597.
    public void test1To1Mapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneTestResources.getResourceURL(
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
        
        assertEquals(comp.toString(), result);
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
        
        assertEquals(comp.toString(), xml);
    }

    public void testCollectionMapping() throws Exception {
        XMLEncoder encoder = new XMLEncoder(CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "collection-mapping.xml").toExternalForm());
        TestObject george = new TestObject();
        george.setName("George");
        george.addChild(new TestObject("Bill", 34, true));

        TestObject sue = new TestObject("Sue", 31, false);
        sue.addChild(new TestObject("Mike", 3, true));
        george.addChild(sue);

        String result = encoder.encode(george);

        String comp = loadTestFileAsString("collection-mapped.xml");

        assertEquals(comp.toString(), result);
    }

    public void testEncodeDataObjectsList() throws Exception {
        List dataObjects = new ArrayList();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        String xml = new XMLEncoder().encode("EncodedTestList", dataObjects);

        String comp = loadTestFileAsString("data-objects-encoded.xml");
        
        assertEquals(comp.toString(), xml);
    }

    public void testDataObjectsListMapping() throws Exception {
        List dataObjects = new ArrayList();

        dataObjects.add(new TestObject("George", 5, true));
        dataObjects.add(new TestObject("Mary", 28, false));
        dataObjects.add(new TestObject("Joe", 31, true));

        String xml = new XMLEncoder(CayenneTestResources.getResourceURL(
                XML_DATA_DIR + "simple-mapping.xml").toExternalForm()).encode(
                "EncodedTestList",
                dataObjects);

        String comp = loadTestFileAsString("data-objects-mapped.xml");

        assertEquals(comp.toString(), xml);
    }
}
