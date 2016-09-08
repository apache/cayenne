package org.apache.cayenne.access.translator.select; /*****************************************************************
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

import junit.framework.TestCase;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeFactory;
import org.apache.cayenne.configuration.DefaultRuntimeProperties;
import org.apache.cayenne.configuration.XMLDataMapLoader;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.dba.JdbcAdapter;
import org.apache.cayenne.exp.parser.ASTObjPath;
import org.apache.cayenne.exp.parser.AggregationFunction;
import org.apache.cayenne.exp.parser.AggregationFunction.Function;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.resource.FilesystemResourceLocator;
import org.apache.cayenne.resource.URLResource;
import org.apache.cayenne.testdo.testmap.Artist;
import org.junit.Test;

import java.io.File;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.regex.Pattern;

import static org.apache.cayenne.testdo.testmap.auto._Artist.*;

public class SelectTranslatorTest extends TestCase {

    @Test
    public void testAggregationForSelectQuery() throws Exception {
        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setResult(new AggregationFunction(Function.COUNT, new ASTObjPath("paintingArray"), "cc"));

        assertEquals("SELECT count(t1.PAINTING_ID) as cc FROM ARTIST t0 JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID)",
                new SelectTranslator(query, getDataNode(), null).createSqlString());
    }

    @Test
    public void testAggregationForSelectQuery_withoutAlias() throws Exception {
        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setResult(new AggregationFunction(Function.COUNT, new ASTObjPath("paintingArray")));

        assertEquals("SELECT count(t1.PAINTING_ID) FROM ARTIST t0 JOIN PAINTING t1 ON (t0.ARTIST_ID = t1.ARTIST_ID)",
                new SelectTranslator(query, getDataNode(), null).createSqlString());
    }

    @Test
    public void testAggregationForSelectQuery_withoutAliasAndProperty() throws Exception {
        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setResult(new AggregationFunction(Function.COUNT));

        assertEquals("SELECT count(*) FROM ARTIST t0",
                new SelectTranslator(query, getDataNode(), null).createSqlString());
    }

    @Test
    public void testQueryResultWithSingleColumn() throws Exception {
        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setResult(ARTIST_NAME);

        assertEquals("SELECT t0.ARTIST_NAME FROM ARTIST t0",
                new SelectTranslator(query, getDataNode(), null).createSqlString());
    }

    @Test
    public void testAggregationForSelectQuery_withProperties() throws Exception {
        SelectQuery<Artist> query = SelectQuery.query(Artist.class);
        query.setResult(new AggregationFunction(Function.COUNT), ARTIST_NAME, DATE_OF_BIRTH);

        assertEquals("SELECT t0.ARTIST_NAME, t0.DATE_OF_BIRTH, count(*) FROM ARTIST t0 GROUP BY t0.ARTIST_NAME, t0.DATE_OF_BIRTH",
                new SelectTranslator(query, getDataNode(), null).createSqlString());
    }

    private DataNode getDataNode() throws URISyntaxException {
        DataNode dataNode = new DataNode();
        DataMap dataMap = new XMLDataMapLoader().load(
                new URLResource(getClass().getClassLoader().getResource("testmap.map.xml")));
        dataNode.setEntityResolver(new EntityResolver(Collections.singleton(dataMap)));

        DbAdapter adapter = new JdbcAdapter(
                new DefaultRuntimeProperties(new HashMap<String, String>()),
                new ArrayList<ExtendedType>(),
                new ArrayList<ExtendedType>(),
                new ArrayList<ExtendedTypeFactory>(),
                new FilesystemResourceLocator(new File(getClass().getClassLoader().getResource(".").toURI())));

        dataNode.setAdapter(adapter);
        return dataNode;
    }

    private void assertMatch(String pattern, String string) {
        assertTrue(Pattern.compile(pattern).matcher(string).matches());
    }

}