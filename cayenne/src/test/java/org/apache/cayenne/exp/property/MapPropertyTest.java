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

package org.apache.cayenne.exp.property;

import java.util.Arrays;

import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * @since 4.2
 */
public class MapPropertyTest {

    private MapProperty<Integer, Artist> property;
    private MapProperty<Integer, Artist> property1;

    @Before
    public void createProperty() {
        property = new MapProperty<>(CayennePath.of("path"), null, Integer.class, Artist.class);
        property1 = new MapProperty<>(CayennePath.of("path.artist"), null, Integer.class, Artist.class);
    }

    @Test
    public void flat() {
        Expression exp = property.flat().getExpression();
        assertEquals(ExpressionFactory.fullObjectExp(ExpressionFactory.pathExp("path")), exp);
    }

    @Test
    public void containsOne() {
        Artist artist = new Artist();
        Expression exp = property.contains(artist);
        assertEquals(ExpressionFactory.matchExp("path", artist), exp);
    }

    @Test
    public void notContainsOne() {
        Artist artist = new Artist();
        Expression exp = property.notContains(artist);
        assertEquals(ExpressionFactory.noMatchExp("path", artist), exp);
    }

    @Test
    public void containsManyArray() {
        Artist artist1 = new Artist();
        Artist artist2 = new Artist();
        Expression exp = property.containsValues(artist1, artist2);
        assertEquals(ExpressionFactory.inExp("path", Arrays.asList(artist1, artist2)), exp);
    }

    @Test
    public void containsManyCollection() {
        Artist artist1 = new Artist();
        Artist artist2 = new Artist();
        Expression exp = property.containsValuesCollection(Arrays.asList(artist1, artist2));
        assertEquals(ExpressionFactory.inExp("path", Arrays.asList(artist1, artist2)), exp);
    }

    @Test
    public void notContainsManyArray() {
        Artist artist1 = new Artist();
        Artist artist2 = new Artist();
        Expression exp = property.notContainsValues(artist1, artist2);
        assertEquals(ExpressionFactory.notInExp("path", Arrays.asList(artist1, artist2)), exp);
    }

    @Test
    public void notContainsManyCollection() {
        Artist artist1 = new Artist();
        Artist artist2 = new Artist();
        Expression exp = property.notContainsValuesCollection(Arrays.asList(artist1, artist2));
        assertEquals(ExpressionFactory.notInExp("path", Arrays.asList(artist1, artist2)), exp);
    }

    @Test
    public void containsOneId() {
        Expression exp = property.containsId(1);
        assertEquals(ExpressionFactory.exp("path = 1"), exp);
    }

    @Test
    public void containsManyIdArray() {
        Expression exp = property.containsIds(1, 2, 3);
        assertEquals(ExpressionFactory.exp("path in (1,2,3)"), exp);
    }

    @Test
    public void containsManyIdCollection() {
        Expression exp = property.containsIdsCollection(Arrays.asList(1, 2, 3));
        assertEquals(ExpressionFactory.exp("path in (1,2,3)"), exp);
    }

    @Test
    public void notContainsOneId() {
        Expression exp = property.notContainsId(1);
        assertEquals(ExpressionFactory.exp("path != 1"), exp);
    }

    @Test
    public void notContainsManyIdArray() {
        Expression exp = property.notContainsIds(1, 2, 3);
        assertEquals(ExpressionFactory.exp("path not in (1,2,3)"), exp);
    }

    @Test
    public void notContainsManyIdCollection() {
        Expression exp = property.notContainsIdsCollection(Arrays.asList(1, 2, 3));
        assertEquals(ExpressionFactory.exp("path not in (1,2,3)"), exp);
    }

    @Test
    public void alias() {
        assertEquals("path", property.getName());
        property = property.alias("alias");
        assertEquals("alias", property.getName());
        assertEquals(1, property.getExpression().getPathAliases().size());

        assertEquals("path.artist", property1.getName());
        property1 = property1.alias("a");
        assertEquals("path.a", property1.getName());
        assertEquals(1, property1.getExpression().getPathAliases().size());
        assertEquals("artist", property1.getExpression().getPathAliases().get("a"));
    }

    @Test
    public void outer() {
        assertEquals("path", property.getName());
        assertEquals(ExpressionFactory.pathExp("path"), property.getExpression());

        property = property.outer();

        assertEquals("path+", property.getName());
        assertEquals(ExpressionFactory.pathExp("path+"), property.getExpression());

        property = property.outer();

        assertEquals("path+", property.getName());
        assertEquals(ExpressionFactory.pathExp("path+"), property.getExpression());
    }
}