/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.wocompat;

import org.apache.cayenne.wocompat.unit.WOCompatCase;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class PropertyListSerializationTest extends WOCompatCase {

    @Test
    public void listPlist() throws Exception {
        File plistFile = new File(setupTestDirectory("listPlist"), "test-array.plist");
        List<Object> list = new ArrayList<>();
        list.add("str");
        list.add(5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void mapPlist() throws Exception {
        File plistFile = new File(setupTestDirectory("mapPlist"), "test-map.plist");
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val");
        map.put("key2", 5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertEquals(map, readMap);
    }

    @Test
    public void emptyString() throws Exception {
        File plistFile = new File(
                setupTestDirectory("emptyString"),
                "test-empty-string.plist");
        Map<String, Object> map = new HashMap<>();
        map.put("a", "");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertEquals(map, readMap);
    }

    @Test
    public void stringWithQuotes() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithQuotes"),
                "test-quotes.plist");
        List<Object> list = new ArrayList<>();
        list.add("s\"tr");
        list.add(5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void nestedPlist() throws Exception {
        File plistFile = new File(
                setupTestDirectory("nestedPlist"),
                "test-nested.plist");
        Map<String, Object> map = new HashMap<>();
        map.put("key1", "val");
        map.put("key2", 5);

        List<Object> list = new ArrayList<>();
        list.add("str");
        list.add(5);
        map.put("key3", list);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, map);
        assertTrue(plistFile.exists());

        Object readMap = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readMap instanceof Map);
        assertEquals(map, readMap);
    }

    @Test
    public void stringWithSpaces() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithSpaces"),
                "test-spaces.plist");
        List<Object> list = new ArrayList<>();
        list.add("s tr");
        list.add(5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void stringWithBraces() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithBraces"),
                "test-braces.plist");
        List<Object> list = new ArrayList<>();
        list.add("s{t)r");
        list.add(5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void stringWithSlashes() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithSlashes"),
                "test-slashes.plist");
        List<Object> list = new ArrayList<>();
        list.add("s/t\\r");
        list.add(5);

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void stringWithQuotes1() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithQuotes1"),
                "test-quotes1.plist");
        List<Object> list = new ArrayList<>();
        list.add("like");
        list.add("key");
        list.add("\"*003*\"");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void stringWithPlusMinus() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithPlusMinus"),
                "test-plus-minus.plist");
        List<Object> list = new ArrayList<>();
        list.add("a+b");
        list.add("a-b");
        list.add("a+-b");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }

    @Test
    public void stringWithLessGreater() throws Exception {
        File plistFile = new File(
                setupTestDirectory("stringWithLessGreater"),
                "test-less-greater.plist");
        List<Object> list = new ArrayList<>();
        list.add("a<b");
        list.add("a>b");
        list.add("a<>b");

        assertFalse(plistFile.exists());
        PropertyListSerialization.propertyListToFile(plistFile, list);
        assertTrue(plistFile.exists());

        Object readList = PropertyListSerialization.propertyListFromFile(plistFile);
        assertTrue(readList instanceof List);
        assertEquals(list, readList);
    }
}
