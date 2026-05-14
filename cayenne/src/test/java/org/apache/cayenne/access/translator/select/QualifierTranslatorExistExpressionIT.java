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

import org.apache.cayenne.access.sqlbuilder.SQLGenerationVisitor;
import org.apache.cayenne.access.sqlbuilder.StringBuilderAppendable;
import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Artist;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.CayenneTestsEnv;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class QualifierTranslatorExistExpressionIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.TESTMAP_PROJECT);

    @Test
    public void existsSimplePath() {
        Expression exp = ExpressionFactory
                .exp("paintingArray")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE t1.ARTIST_ID = t0.ARTIST_ID" +
                ")", node);
    }

    @Test
    public void existsSimplePathParsed() {
        Expression exp = ExpressionFactory
                .exp("exists paintingArray");
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE t1.ARTIST_ID = t0.ARTIST_ID" +
                ")", node);
    }

    @Test
    public void existsSimplePathNoRelationship() {
        Expression exp = ExpressionFactory
                .exp("artistName")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" t0.ARTIST_NAME IS NOT NULL", node);
    }

    @Test
    public void existsLongPathSimpleAttribute() {
        Expression exp = ExpressionFactory
                .exp("paintingArray.paintingTitle")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE ( t1.PAINTING_TITLE IS NOT NULL ) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsLongToOnePath() {
        Expression exp = ExpressionFactory
                .exp("paintingArray.toGallery")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE ( t1.GALLERY_ID IS NOT NULL ) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsLongToManyPath() {
        Expression exp = ExpressionFactory
                .exp("groupArray.childGroupsArray")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT DISTINCT t1.ARTIST_ID, t1.GROUP_ID FROM ARTIST_GROUP t1 " +
                "JOIN ARTGROUP t2 ON t1.GROUP_ID = t2.GROUP_ID " +
                "JOIN ARTGROUP t3 ON t2.GROUP_ID = t3.PARENT_GROUP_ID " +
                "WHERE ( t3.GROUP_ID IS NOT NULL ) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsSimpleCondition() {
        Expression exp = ExpressionFactory
                .exp("paintingArray.paintingTitle = 'test'")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE ( t1.PAINTING_TITLE = 'test' ) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsAggConditionSameRoot() {
        Expression exp = ExpressionFactory
                .exp("paintingArray.paintingTitle = 'test' " +
                        "or paintingArray.paintingTitle = 'test2'")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE ( ( t1.PAINTING_TITLE = 'test' ) OR ( t1.PAINTING_TITLE = 'test2' ) ) " +
                        "AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsAggConditionSameRootParser() {
        Expression exp = ExpressionFactory
                .exp("exists (paintingArray.paintingTitle = 'test' " +
                        "or paintingArray.paintingTitle = 'test2')");

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                "WHERE ( ( t1.PAINTING_TITLE = 'test' ) OR ( t1.PAINTING_TITLE = 'test2' ) ) " +
                "AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    @Test
    public void existsAggConditionMultipleRoots() {
        Expression exp = ExpressionFactory
                .exp("(paintingArray.paintingTitle = 'test' " +
                        "or paintingArray.paintingTitle = 'test2') and groupArray.name = 'test'")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                    "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                    "WHERE ( ( t1.PAINTING_TITLE = 'test' ) OR ( t1.PAINTING_TITLE = 'test2' ) ) " +
                            "AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ") " +
                "AND EXISTS (" +
                    "SELECT t2.ARTIST_ID, t2.GROUP_ID FROM ARTIST_GROUP t2 " +
                    "JOIN ARTGROUP t3 ON t2.GROUP_ID = t3.GROUP_ID " +
                    "WHERE ( t3.NAME = 'test' ) AND ( t2.ARTIST_ID = t0.ARTIST_ID ))", node);
    }

    @Test
    public void existsAggDifferentRoots() {
        Expression exp = ExpressionFactory
                .exp("paintingArray.paintingTitle = 'test' " +
                        "and (groupArray.name = 'test' or paintingArray.paintingTitle = 'test2')")
                .exists();

        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                    "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                    "WHERE ( t1.PAINTING_TITLE = 'test' ) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ") " +
                "AND ( " +
                    "EXISTS (" +
                        "SELECT t2.ARTIST_ID, t2.GROUP_ID FROM ARTIST_GROUP t2 " +
                        "JOIN ARTGROUP t3 ON t2.GROUP_ID = t3.GROUP_ID " +
                        "WHERE ( t3.NAME = 'test' ) AND ( t2.ARTIST_ID = t0.ARTIST_ID )" +
                    ") " +
                    "OR " +
                    "EXISTS (" +
                        "SELECT t4.PAINTING_ID FROM PAINTING t4 " +
                        "WHERE ( t4.PAINTING_TITLE = 'test2' ) AND ( t4.ARTIST_ID = t0.ARTIST_ID )" +
                    ") " +
                ")", node);
    }

    @Test
    public void existsComplexConditionsDifferentRoots() {
        Expression exp = ExpressionFactory
                .exp("(length(paintingArray.paintingTitle) in (1, 2, 3)) " +
                    "or (length(groupArray.name) < 10)")
                .exists();
        ObjectSelect<Artist> query = ObjectSelect.query(Artist.class, exp);

        DefaultSelectTranslator translator
                = new DefaultSelectTranslator(query, env.runtime().getDataDomain().getDefaultNode().getAdapter(), env.context().getEntityResolver());

        QualifierTranslator qualifierTranslator = translator.getContext().getQualifierTranslator();

        Node node = qualifierTranslator.translate(query.getWhere());

        assertSQL(" EXISTS (" +
                    "SELECT t1.PAINTING_ID FROM PAINTING t1 " +
                    "WHERE LENGTH( t1.PAINTING_TITLE ) IN ( 1, 2, 3) AND ( t1.ARTIST_ID = t0.ARTIST_ID )" +
                ") OR " +
                "EXISTS (" +
                    "SELECT t2.ARTIST_ID, t2.GROUP_ID FROM ARTIST_GROUP t2 " +
                    "JOIN ARTGROUP t3 ON t2.GROUP_ID = t3.GROUP_ID " +
                    "WHERE ( LENGTH( t3.NAME ) < 10 ) AND ( t2.ARTIST_ID = t0.ARTIST_ID )" +
                ")", node);
    }

    protected void assertSQL(String expected, Node node) {
        assertNotNull(node);

        SQLGenerationVisitor visitor = new SQLGenerationVisitor(new StringBuilderAppendable());
        node.visit(visitor);
        assertEquals(expected, visitor.getSQLString());
    }

}
