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

package org.apache.cayenne.gen;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.cayenne.gen.DataMapUtils.QueryParameter;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.map.ObjRelationship;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class DataMapUtilsTest {

    private DataMapUtils dataMapUtils;
    private DataMap dataMap;

    @BeforeEach
    public void setUp() {
        dataMapUtils = new DataMapUtils();
        dataMap = new DataMap("map");

        DbEntity dbEntity = new DbEntity("ARTIST");
        dbEntity.addAttribute(new DbAttribute("NAME"));
        dataMap.addDbEntity(dbEntity);

        ObjEntity artist = new ObjEntity("Artist");
        artist.setClassName("org.example.Artist");
        artist.setDbEntity(dbEntity);
        artist.addAttribute(new ObjAttribute("name", "java.lang.String", artist));
        artist.addAttribute(new ObjAttribute("born", "java.time.LocalDate", artist));
        dataMap.addObjEntity(artist);

        ObjEntity painting = new ObjEntity("Painting");
        painting.setClassName("org.example.Painting");
        painting.addAttribute(new ObjAttribute("price", "java.math.BigDecimal", painting));
        dataMap.addObjEntity(painting);

        ObjRelationship toArtist = new ObjRelationship("artist");
        toArtist.setTargetEntityName("Artist");
        painting.addRelationship(toArtist);
    }

    @Test
    public void getParameters() {
        SelectQueryDescriptor query = query("""
                from Artist where name like $name and born in ($from_date, $to_date) and name != $name""");

        assertEquals(List.of(
                new QueryParameter("name", "name", "java.lang.String"),
                new QueryParameter("from_date", "fromDate", "java.time.LocalDate"),
                new QueryParameter("to_date", "toDate", "java.time.LocalDate")), dataMapUtils.getParameters(query));
        assertTrue(dataMapUtils.hasParameters(query));
        assertFalse(dataMapUtils.hasParameters(query("from Artist")));
    }

    @Test
    public void getParametersCached() {
        SelectQueryDescriptor query = query("from Artist where name = $name");
        List<QueryParameter> parameters = dataMapUtils.getParameters(query);
        assertSame(parameters, dataMapUtils.getParameters(query));

        // a change of the query String is picked up
        query.setQl("from Artist where born = $born");
        assertEquals(List.of(new QueryParameter("born", "born", "java.time.LocalDate")),
                dataMapUtils.getParameters(query));
    }

    @Test
    public void getParametersUniqueJavaNames() {
        SelectQueryDescriptor query = query("""
                from Artist where name = $a_b or name = $aB or name = $context or born = $query or born = $class""");

        assertEquals(List.of("a_b", "aB", "context", "query", "class"), names(query, QueryParameter::name));
        assertEquals(List.of("aB", "aB2", "context2", "query2", "class2"), names(query, QueryParameter::javaName));
    }

    @Test
    public void getParameterType() {
        SelectQueryDescriptor query = query("""
                from Painting where price > $price and artist = $artist and $flag = true
                    and exists (from Artist where born < $born)""");

        assertEquals(Map.of(
                "price", "java.math.BigDecimal",
                "artist", "org.example.Artist",
                "flag", "java.lang.Object",
                "born", "java.time.LocalDate"), types(query));
    }

    @Test
    public void getParameterTypeOfIn() {
        SelectQueryDescriptor query = query("""
                from Artist where name in $names and born not in $dates and name in ($a, $b) and $c in ('x')""");

        assertEquals(Map.of(
                "names", "java.util.Collection<java.lang.String>",
                "dates", "java.util.Collection<java.time.LocalDate>",
                "a", "java.lang.String",
                "b", "java.lang.String",
                "c", "java.lang.Object"), types(query));
    }

    @Test
    public void getParameterTypeOfConflictingUses() {
        SelectQueryDescriptor query = query("""
                from Artist where name = $x and born = $x and name = $y and $y = 1 and $z = 1 and born = $z
                    and name in $w and name = $w""");

        assertEquals(Map.of(
                "x", "java.lang.Object",
                "y", "java.lang.String",
                "z", "java.time.LocalDate",
                "w", "java.lang.Object"), types(query));
    }

    @Test
    public void getParameterTypeOfAlias() {
        SelectQueryDescriptor query = query("""
                select name as n, count(*) as c from Artist having c > $min and n like $pattern""");

        assertTrue(dataMapUtils.isValidParameterNames(query));
        assertEquals(List.of("min", "pattern"), names(query, QueryParameter::javaName));
        assertEquals(Map.of("min", "java.lang.Object", "pattern", "java.lang.String"), types(query));
    }

    @Test
    public void getParameterTypeOfInvalidPath() {
        // an invalid path is reported by the validator, the parameter helpers never fail on it
        SelectQueryDescriptor query = query("from Artist where nme = $name and born = $born");

        assertFalse(dataMapUtils.isValidParameterNames(query));
        assertEquals(List.of("name", "born"), names(query, QueryParameter::javaName));
        assertTrue(dataMapUtils.hasParameters(query));
        assertEquals(Map.of("name", "java.lang.Object", "born", "java.time.LocalDate"), types(query));
    }

    @Test
    @SuppressWarnings("deprecation")
    public void deprecatedParameterMethods() {
        SelectQueryDescriptor query = query("from Artist where born = $from_date");

        assertEquals(List.of("fromDate"), List.copyOf(dataMapUtils.getParameterNames(query)));
        assertEquals("java.time.LocalDate", dataMapUtils.getParameterType(query, "fromDate"));
    }

    @Test
    public void isValidParameterNames() {
        assertTrue(dataMapUtils.isValidParameterNames(query("from Artist where name = $name order by born")));
        assertTrue(dataMapUtils.isValidParameterNames(query("from db:ARTIST where NAME = $name")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from Artist where")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from Gallery")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from Artist where nme = $name")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from Artist order by nme")));

        // the paths that are not compared to a parameter are checked too
        assertFalse(dataMapUtils.isValidParameterNames(query("from Artist where nme = 'x'")));
        assertFalse(dataMapUtils.isValidParameterNames(query("select nme from Artist")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from db:ARTIST where NME = $name")));
        assertFalse(dataMapUtils.isValidParameterNames(query("from Artist prefetch galleries")));
    }

    @Test
    public void getResultType() {
        assertEquals("org.example.Artist", dataMapUtils.getResultType(query("from Artist")));
        assertEquals("org.apache.cayenne.DataRow", dataMapUtils.getResultType(query("from db:ARTIST")));
        assertEquals("java.lang.Object", dataMapUtils.getResultType(query("select name from Artist")));
        assertEquals("java.lang.Object[]", dataMapUtils.getResultType(query("select name, born from Artist")));

        SelectQueryDescriptor dataRows = query("from Artist");
        dataRows.setFetchingDataRows(true);
        assertEquals("org.apache.cayenne.DataRow", dataMapUtils.getResultType(dataRows));
    }

    private List<String> names(SelectQueryDescriptor query, Function<QueryParameter, String> name) {
        return dataMapUtils.getParameters(query).stream().map(name).toList();
    }

    private Map<String, String> types(SelectQueryDescriptor query) {
        return dataMapUtils.getParameters(query).stream()
                .collect(Collectors.toMap(QueryParameter::javaName, QueryParameter::type));
    }

    private SelectQueryDescriptor query(String ql) {
        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setName("q");
        query.setQl(ql);
        dataMap.removeQueryDescriptor("q");
        dataMap.addQueryDescriptor(query);
        return query;
    }
}
