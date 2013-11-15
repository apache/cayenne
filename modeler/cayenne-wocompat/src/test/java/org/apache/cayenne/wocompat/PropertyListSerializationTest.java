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

package org.apache.cayenne.wocompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.wocompat.unit.WOCompatCase;

public class PropertyListSerializationTest extends WOCompatCase {

    public void testListPlist() throws Exception {
        File plistFile = new File(setupTestDirectory("testListPlist"), "test-array.plist");
        List<Object> list = new ArrayList<Object>();
        list.add("str");
        list.add(new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testMapPlist() throws Exception {
        File plistFile = new File(setupTestDirectory("testMapPlist"), "test-map.plist");
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertTrue(map.equals(readMap));
    }

    public void testEmptyString() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testEmptyString"),
                "test-empty-string.plist");
        Map map = new HashMap();
        map.put("a", "");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertTrue(map.equals(readMap));
    }

    public void testStringWithQuotes() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithQuotes"),
                "test-quotes.plist");
        List<Object> list = new ArrayList<Object>();
        list.add("s\"tr");
        list.add(new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testNestedPlist() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testNestedPlist"),
                "test-nested.plist");
        Map map = new HashMap();
        map.put("key1", "val");
        map.put("key2", new Integer(5));

        List list = new ArrayList();
        list.add("str");
        list.add(new Integer(5));
        map.put("key3", list);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertTrue(map.equals(readMap));
    }

    public void testStringWithSpaces() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithSpaces"),
                "test-spaces.plist");
        List list = new ArrayList();
        list.add("s tr");
        list.add(new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testStringWithBraces() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithBraces"),
                "test-braces.plist");
        List list = new ArrayList();
        list.add("s{t)r");
        list.add(new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testStringWithSlashes() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithSlashes"),
                "test-slashes.plist");
        List list = new ArrayList();
        list.add("s/t\\r");
        list.add(new Integer(5));

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testStringWithQuotes1() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithQuotes1"),
                "test-quotes1.plist");
        List list = new ArrayList();
        list.add("like");
        list.add("key");
        list.add("\"*003*\"");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testStringWithPlusMinus() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithPlusMinus"),
                "test-plus-minus.plist");
        List list = new ArrayList();
        list.add("a+b");
        list.add("a-b");
        list.add("a+-b");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }

    public void testStringWithLessGreater() throws Exception {
        File plistFile = new File(
                setupTestDirectory("testStringWithLessGreater"),
                "test-less-greater.plist");
        List list = new ArrayList();
        list.add("a<b");
        list.add("a>b");
        list.add("a<>b");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertTrue(list.equals(readList));
    }
}
