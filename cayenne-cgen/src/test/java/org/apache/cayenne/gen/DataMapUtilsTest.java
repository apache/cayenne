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
    public void getParameterNames() {
        SelectQueryDescriptor query = query("""
                from Artist where name like $name and born in ($from_date, $to_date) and name != $name""");

        assertEquals(List.of("name", "fromDate", "toDate"), List.copyOf(dataMapUtils.getParameterNames(query)));
        assertTrue(dataMapUtils.hasParameters(query));
        assertFalse(dataMapUtils.hasParameters(query("from Artist")));
    }

    @Test
    public void getParameterType() {
        SelectQueryDescriptor query = query("""
                from Painting where price > $price and artist = $artist and $flag = true
                    and exists (from Artist where born < $born)""");

        assertEquals("java.math.BigDecimal", dataMapUtils.getParameterType(query, "price"));
        assertEquals("org.example.Artist", dataMapUtils.getParameterType(query, "artist"));
        assertEquals("java.lang.Object", dataMapUtils.getParameterType(query, "flag"));
        assertEquals("java.time.LocalDate", dataMapUtils.getParameterType(query, "born"));
    }

    @Test
    public void getParameterTypeOfAlias() {
        SelectQueryDescriptor query = query("""
                select name as n, count(*) as c from Artist having c > $min and n like $pattern""");

        assertTrue(dataMapUtils.isValidParameterNames(query));
        assertEquals(List.of("min", "pattern"), List.copyOf(dataMapUtils.getParameterNames(query)));
        assertEquals("java.lang.Object", dataMapUtils.getParameterType(query, "min"));
        assertEquals("java.lang.String", dataMapUtils.getParameterType(query, "pattern"));
    }

    @Test
    public void getParameterTypeOfInvalidPath() {
        // an invalid path is reported by the validator, the parameter helpers never fail on it
        SelectQueryDescriptor query = query("from Artist where nme = $name and born = $born");

        assertFalse(dataMapUtils.isValidParameterNames(query));
        assertEquals(List.of("name", "born"), List.copyOf(dataMapUtils.getParameterNames(query)));
        assertTrue(dataMapUtils.hasParameters(query));
        assertEquals("java.lang.Object", dataMapUtils.getParameterType(query, "name"));
        assertEquals("java.time.LocalDate", dataMapUtils.getParameterType(query, "born"));
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

    private SelectQueryDescriptor query(String ql) {
        SelectQueryDescriptor query = new SelectQueryDescriptor();
        query.setName("q");
        query.setQl(ql);
        dataMap.removeQueryDescriptor("q");
        dataMap.addQueryDescriptor(query);
        return query;
    }
}
