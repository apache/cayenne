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

package org.apache.cayenne.query.resolved;

import org.apache.cayenne.ObjectId;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.types.ValueObjectType;
import org.apache.cayenne.access.types.ValueObjectTypeRegistry;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.TraversalHandler;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.query.PrefetchProcessor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.apache.cayenne.access.types.DefaultValueObjectTypeRegistry;
import org.apache.cayenne.unit.TestObjects;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * This class is testing converting Expressions to cache key part.
 */
public class ToCacheKeyTraversalHandlerTest {

    private ValueObjectTypeRegistry registry;
    private StringBuilder cacheKey;

    @BeforeEach
    public void createObjects() {
        registry = new DefaultValueObjectTypeRegistry(List.of(new PlaceholderDoubleType(), new TestValueType()));
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
        ObjectId objectId1 = ObjectId.of("E", "ID", 1);
        Persistent persistent1 = TestObjects.committedObject(objectId1);

        ObjectId objectId2 = ObjectId.of("E", "ID", 2);
        Persistent persistent2 = TestObjects.committedObject(objectId2);

        ExpressionFactory.greaterOrEqualExp("testPath", persistent1).traverse(newHandler());
        String s1 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", persistent1).traverse(newHandler());
        String s2 = cacheKey.toString();

        ExpressionFactory.greaterOrEqualExp("testPath", persistent2).traverse(newHandler());
        String s3 = cacheKey.toString();

        assertTrue(s1.contains(objectId1.toString()));
        assertTrue(s3.contains(objectId2.toString()));
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
    public void prefetchEmpty() {
        PrefetchTreeNode prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.traverse(newPrefetchProcessor());
        assertTrue(cacheKey.toString().isEmpty());
    }

    @Test
    public void prefetchSingle() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        assertFalse(s1.isEmpty());
        assertEquals(s1, s2);
    }

    @Test
    public void prefetchSemantics() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.disjoint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.disjointById());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.disjoint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s4 = cacheKey.toString();

        assertNotEquals(s1, s2);
        assertNotEquals(s2, s3);
        assertEquals(s2, s4);
    }

    @Test
    public void prefetchMultiNodes() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.merge(Artist.GROUPS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.merge(Artist.GROUPS.joint());
        prefetchTreeNode.merge(Artist.ARTIST_EXHIBITS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.merge(Artist.GROUPS.joint());
        prefetchTreeNode.merge(Artist.ARTIST_EXHIBITS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s4 = cacheKey.toString();

        assertNotEquals(s1, s2);
        assertNotEquals(s2, s3);
        assertEquals(s3, s4);
    }

    @Test
    public void prefetchLongPaths() {
        PrefetchTreeNode prefetchTreeNode;

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s1 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.dot(Painting.ARTIST).joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s2 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.dot(Painting.ARTIST).dot(Artist.GROUPS).joint());
        prefetchTreeNode.traverse(newPrefetchProcessor());
        String s3 = cacheKey.toString();

        prefetchTreeNode = new PrefetchTreeNode();
        prefetchTreeNode.merge(Artist.PAINTINGS.dot(Painting.ARTIST).dot(Artist.GROUPS).joint());
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
     * Marks Double as a custom value object, with a fixed cache key
     */
    static class PlaceholderDoubleType implements ValueObjectType<Double, Double> {
        @Override
        public Class<Double> getTargetType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<Double> getValueType() {
            return Double.class;
        }

        @Override
        public Double toJavaObject(Double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        public Double fromJavaObject(Double object) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String toCacheKey(Double object) {
            return "<value placeholder>";
        }
    }

    /**
     * Test value object descriptor, we need only getValueType() and toCacheKey() methods
     */
    static class TestValueType implements ValueObjectType<TestValue, Integer> {
        @Override
        public Class<Integer> getTargetType() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Class<TestValue> getValueType() {
            return TestValue.class;
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