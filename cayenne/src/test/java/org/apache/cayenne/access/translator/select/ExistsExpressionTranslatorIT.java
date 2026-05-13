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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.exp.parser.SimpleNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.QueryMetadata;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ExistsExpressionTranslatorIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    private ObjectContext context;

    private TranslatorContext translatorContext;

    @BeforeEach
    public void setUp() {
        context = env.context();
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
    public void simplePath() {
        Expression exp = ExpressionFactory.exp("paintingArray").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void simplePathNoRelationship() {
        Expression exp = ExpressionFactory.exp("artistName").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void simpleLongPath() {
        Expression exp = ExpressionFactory.exp("paintingArray.toGallery").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void simpleCondition() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void simpleConditionsSameRoot() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test' " +
                "or paintingArray.paintingTitle = 'test2'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void simpleConditionsDifferentRoots() {
        Expression exp = ExpressionFactory.exp("paintingArray.paintingTitle = 'test' " +
                "or groupArray.name = 'test'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void complexCondition() {
        Expression exp = ExpressionFactory.exp("length(paintingArray.paintingTitle) in (1, 2, 3)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void complexConditionsSameRoot() {
        Expression exp = ExpressionFactory.exp("(length(paintingArray.paintingTitle) in (1, 2, 3)) " +
                "or (paintingArray.estimatedPrice > 10000)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void complexConditionsDifferentRoots() {
        Expression exp = ExpressionFactory.exp("(length(paintingArray.paintingTitle) in (1, 2, 3)) " +
                "or (length(groupArray.name) < 10)").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
    }

    @Test
    public void noRelationships() {
        Expression exp = ExpressionFactory.exp("artistName like 'test%'").exists();
        Expression translated = new ExistsExpressionTranslator(translatorContext, (SimpleNode)exp).translate();

        assertNotNull(translated);
        assertEquals("db:ARTIST_NAME like \"test%\"", translated.toString());
    }

    @Test
    public void dbPath() {
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