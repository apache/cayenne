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

package org.apache.cayenne.query;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * This class is testing converting Expressions to cache key part.
 *
 * @since 4.0
 */
public class SelectQueryMetadataCacheKeyTest {

    private ValueObjectTypeRegistry registry;
    private StringBuilder cacheKey;

    @SuppressWarnings("unchecked")
    @Before
    public void createObjects() {
        registry = mock(ValueObjectTypeRegistry.class);

        // mock value type for Double class
        ValueObjectType mockType = mock(ValueObjectType.class);
        when(mockType.getValueType()).thenReturn(Double.class);
        when(mockType.toCacheKey(any())).thenReturn("<value placeholder>");
        when(registry.getValueType(eq(Double.class))).thenReturn(mockType);

        // value type for TestValue class
        ValueObjectType testType = new TestValueType();
        when(registry.getValueType(eq(TestValue.class))).thenReturn(testType);
    }

    /**
     * Simple expressions
     */
    @Test
    public void cacheKeySimple() {
        ExpressionFactory.exp("field = 1").traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.exp("field = 1").traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.exp("field = 2").traverse(newHandler());
        String s3 = cacheKey.toString();

        assertEquals(s1, s2);
        assertNotEquals(s2, s3);
    }

    /**
     * Expressions with list of simple values
     */
    @Test
    public void cacheKeyWithList() {
        ExpressionFactory.exp("field in (1,2,3)").traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.exp("field in (1,2,3)").traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.exp("field in (2,3,4)").traverse(newHandler());
        String s3 = cacheKey.toString();

        assertEquals(s1, s2);
        assertNotEquals(s2, s3);
    }

    /**
     * Simple test for custom value object, Double.class is marked as a custom value object.
     */
    @Test
    public void cacheKeyWithValueObjectSimple() {
        ExpressionFactory.exp("field = 1.0").traverse(newHandler());
        String s1 = cacheKey.toString();

        assertTrue(s1.contains("<value placeholder>"));
    }

    /**
     * List of value objects, Double.class is marked as a custom value object.
     */
    @Test
    public void cacheKeyWithValueObjectList() {
        ExpressionFactory.exp("field in (1.0,2.0,3.0)").traverse(newHandler());
        String s1 = cacheKey.toString();

        assertTrue(s1.contains("<value placeholder>"));
    }

    @Test
    public void cacheKeyWithEnumValue() {
        ExpressionFactory.greaterOrEqualExp("testPath", TestEnum.VALUE_1).traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", TestEnum.VALUE_1).traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", TestEnum.VALUE_2).traverse(newHandler());
        String s3 = cacheKey.toString();

        assertEquals(s1, s2);
        assertNotEquals(s2, s3);
    }

    @Test
    public void cacheKeyWithValueObject() {
        ExpressionFactory.greaterOrEqualExp("testPath", new TestValue(1)).traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", new TestValue(1)).traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", new TestValue(2)).traverse(newHandler());
        String s3 = cacheKey.toString();

        assertEquals(s1, s2);
        assertNotEquals(s2, s3);
    }

    /**
     * Persistent objects should be converted to their ObjectIds.
     */
    @Test
    public void cacheKeyWithPersistentObject() {
        Persistent persistent1 = mock(Persistent.class);
        ObjectId objectId1 = mock(ObjectId.class);
        when(objectId1.toString()).thenReturn("objId1");
        when(persistent1.getObjectId()).thenReturn(objectId1);

        Persistent persistent2 = mock(Persistent.class);
        ObjectId objectId2 = mock(ObjectId.class);
        when(objectId2.toString()).thenReturn("objId2");
        when(persistent2.getObjectId()).thenReturn(objectId2);

        ExpressionFactory.greaterOrEqualExp("testPath", persistent1).traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", persistent1).traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", persistent2).traverse(newHandler());
        String s3 = cacheKey.toString();

        assertTrue(s1.contains("objId1"));
        assertTrue(s3.contains("objId2"));
        assertEquals(s1, s2);
        assertNotEquals(s2, s3);
    }

    @Test
    public void cacheKeyWithFunctionCall() {
        ExpressionFactory.exp("length(testPath)").traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.exp("length(testPath)").traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.exp("count(testPath)").traverse(newHandler());
        String s3 = cacheKey.toString();

        assertEquals(s1, s2);
        assertNotEquals(s2, s3);

        ExpressionFactory.exp("substring(path, testPath)").traverse(newHandler());
        String s4 = cacheKey.toString();

        ExpressionFactory.exp("substring(path2, testPath)").traverse(newHandler());
        String s5 = cacheKey.toString();

        assertNotEquals(s4, s5);

        ExpressionFactory.exp("year(path)").traverse(newHandler());
        String s6 = cacheKey.toString();

        ExpressionFactory.exp("hour(path)").traverse(newHandler());
        String s7 = cacheKey.toString();

        assertNotEquals(s6, s7);
    }

    @Test
    public void testPrefetchEmpty() {
        PrefetchTreeNode prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.traverse(newPrefetchProcessor());
        assertTrue(cacheKey.toString().isEmpty());
    }

    @Test
    public void testPrefetchSingle() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        assertFalse(s1.isEmpty());
        assertEquals(s1, s2);
    }

    @Test
    public void testPrefetchSemantics() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.disjoint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.disjointById());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.disjoint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s4 = cacheKey.toString();

        assertNotEquals(s1, s2);
        assertNotEquals(s2, s3);
        assertEquals(s2, s4);
    }

    @Test
    public void testPrefetchMultiNodes() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.merge(Artist.GROUP_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.merge(Artist.GROUP_ARRAY.joint());
        prefetchTreeNode.merge(Artist.ARTIST_EXHIBIT_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.merge(Artist.GROUP_ARRAY.joint());
        prefetchTreeNode.merge(Artist.ARTIST_EXHIBIT_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s4 = cacheKey.toString();

        assertNotEquals(s1, s2);
        assertNotEquals(s2, s3);
        assertEquals(s3, s4);
    }

    @Test
    public void testPrefetchLongPaths() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.dot(Painting.TO_ARTIST).joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.dot(Painting.TO_ARTIST).dot(Artist.GROUP_ARRAY).joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTING_ARRAY.dot(Painting.TO_ARTIST).dot(Artist.GROUP_ARRAY).joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s4 = cacheKey.toString();

        assertNotEquals(s1, s2);
        assertNotEquals(s2, s3);
        assertEquals(s3, s4);
    }

    private TraversalHandler newHandler() {
        return new ToCacheKeyTraversalHandler(registry, cacheKey = new StringBuilder());
    }

    private PrefetchProcessor newPrefetchProcessor() {
        return new ToCacheKeyPrefetchProcessor(cacheKey = new StringBuilder());
    }

    /* ************* Test types *************** */

    /**
     * Test enum
     */
    enum TestEnum { VALUE_1, VALUE_2 }

    /**
     * Test value object
     */
    static class TestValue {
        int v = 0;
        TestValue(int v) {
            this.v = v;
        }
    }

    /**
     * Test value object descriptor, we need only toCacheKey() method
     */
    static class TestValueType implements ValueObjectType<TestValue, Integer> {
        @Override
        public Class<Integer> getTargetType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<TestValue> getValueType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TestValue toJavaObject(Integer value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Integer fromJavaObject(TestValue object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toCacheKey(TestValue object) {
            return Integer.toString(object.v);
        }
    }
}