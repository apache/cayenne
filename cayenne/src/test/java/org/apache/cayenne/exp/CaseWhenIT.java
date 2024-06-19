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

package org.apache.cayenne.exp;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.property.PropertyFactory;
import org.apache.cayenne.exp.property.StringProperty;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.test.jdbc.TableHelper;
import org.apache.cayenne.testdo.testmap.Painting;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.apache.cayenne.exp.ExpressionFactory.betweenExp;
import static org.apache.cayenne.exp.ExpressionFactory.caseWhen;
import static org.apache.cayenne.exp.ExpressionFactory.wrapScalarValue;

@UseCayenneRuntime(CayenneProjects.TESTMAP_PROJECT)
public class CaseWhenIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    protected DBHelper dbHelper;

    @Before
    public void setUp() throws Exception {
        new TableHelper(dbHelper, "PAINTING")
                .setColumns("PAINTING_ID", "PAINTING_TITLE", "PAINTING_DESCRIPTION", "ESTIMATED_PRICE")
                .insert(1, "Black square", "Oil on linen, 79.5 x 79.5 cm", 15);
    }

    @Test
    public void caseWhenExpressionFactoryTest() {
        Expression caseWhenNoDefault = caseWhen(
                List.of((betweenExp("estimatedPrice", 0, 9)),
                        (betweenExp("estimatedPrice", 10, 20))),
                List.of((wrapScalarValue("firstThenResult")),
                        (wrapScalarValue("secondThenResult"))));

        StringProperty<String> propertyNoDefault = PropertyFactory.createString(caseWhenNoDefault, String.class);
        String result = ObjectSelect.columnQuery(Painting.class, propertyNoDefault).selectFirst(context);
        Assert.assertEquals("secondThenResult", result);
    }

    @Test
    public void caseWhenDefaultExpressionFactoryTest() {
        Expression caseWhenNoDefault = caseWhen(
                List.of(betweenExp("estimatedPrice", 0, 14)),
                List.of((wrapScalarValue("firstThenResult"))),
                wrapScalarValue("defaultResult"));

        StringProperty<String> propertyNoDefault = PropertyFactory.createString(caseWhenNoDefault, String.class);
        String result = ObjectSelect.columnQuery(Painting.class, propertyNoDefault).selectFirst(context);
        Assert.assertEquals("defaultResult", result.trim());

    }

}
