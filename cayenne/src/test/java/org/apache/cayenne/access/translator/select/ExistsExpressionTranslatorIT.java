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

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class ExistsExpressionTranslatorIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    private TranslatorContext translatorContext;

    @Before
    public void setUp() {
        translatorContext = mock(TranslatorContext.class);
        DbEntity dbArtist = context.getEntityResolver().getDbEntity("ARTIST");
        ObjEntity objArtist = context.getEntityResolver().getObjEntity("Artist");
        when(translatorContext.getRootDbEntity()).thenReturn(dbArtist);
        QueryMetadata metadata = new MockQueryMetadata() {
            @Override
            public ObjEntity getObjEntity() {
                return objArtist;
            }
        };
        when(translatorContext.getMetadata()).thenReturn(metadata);
    }

    @Test
    public void testSimplePath() {
        Expression exp = ExpressionFactory.exp("paintingArray").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testSimplePathNoRelationship() {
        Expression exp = ExpressionFactory.exp("artistName").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testSimpleLongPath() {
        Expression exp = ExpressionFactory.exp("paintingArray.toGallery").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testSimpleCondition() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testSimpleConditionsSameRoot() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test' " +
                "or paintingArray.paintingTitle = 'test2'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testSimpleConditionsDifferentRoots() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test' " +
                "or groupArray.name = 'test'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testComplexCondition() {
        Expression exp = ExpressionFactory.exp("length(paintingArray.paintingTitle) in (1, 2, 3)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testComplexConditionsSameRoot() {
        Expression exp = ExpressionFactory.exp("(length(paintingArray.paintingTitle) in (1, 2, 3)) " +
                "or (paintingArray.estimatedPrice > 10000)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testComplexConditionsDifferentRoots() {
        Expression exp = ExpressionFactory.exp("(length(paintingArray.paintingTitle) in (1, 2, 3)) " +
                "or (length(groupArray.name) < 10)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void testNoRelationships() {
        Expression exp = ExpressionFactory.exp("artistName like 'test%'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
        assertEquals("db:ARTIST_NAME like \"test%\"", translated.toString());
    }

    @Test
    public void testDbPath() {
        Expression exp = ExpressionFactory.exp("db:PAINTING_ARRAY").exists();

        translatorContext = mock(TranslatorContext.class);
        DbEntity dbArtist = context.getEntityResolver().getDbEntity("ARTIST");
        when(translatorContext.getRootDbEntity()).thenReturn(dbArtist);
        when(translatorContext.getMetadata()).thenReturn(new MockQueryMetadata());

        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
        assertEquals("db:PAINTING_ARRAY != null", translated.toString());
    }
}