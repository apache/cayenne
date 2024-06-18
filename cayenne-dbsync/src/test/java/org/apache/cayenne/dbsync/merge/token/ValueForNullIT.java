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

package org.apache.cayenne.dbsync.merge.token;

import java.sql.Types;
import java.util.List;

import junit.framework.AssertionFailedError;
import org.apache.cayenne.Persistent;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.translator.ParameterBinding;
import org.apache.cayenne.dbsync.merge.DataMapMerger;
import org.apache.cayenne.dbsync.merge.MergeCase;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.ObjAttribute;
import org.apache.cayenne.map.ObjEntity;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.testmap.Painting;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ValueForNullIT extends MergeCase {

    private static final String DEFAULT_VALUE_STRING = "DEFSTRING";

    @Inject
    private DataContext context;

    @Test
    public void test() throws Exception {
        DbEntity dbEntity = map.getDbEntity("PAINTING");
        assertNotNull(dbEntity);
        ObjEntity objEntity = map.getObjEntity("Painting");
        assertNotNull(objEntity);

        // insert some rows before adding "not null" column
        final int nrows = 10;
        for (int i = 0; i < nrows; i++) {
            Persistent o = context.newObject("Painting");
            o.writeProperty("paintingTitle", "ptitle" + i);
        }
        context.commitChanges();

        // create and add new column to model and db
        DbAttribute column = new DbAttribute("NEWCOL2", Types.VARCHAR, dbEntity);

        column.setMandatory(false);
        column.setMaxLength(10);
        dbEntity.addAttribute(column);
        assertTrue(dbEntity.getAttributes().contains(column));
        assertEquals(column, dbEntity.getAttribute(column.getName()));
        assertTokensAndExecute(1, 0);

        // need obj attr to be able to query
        ObjAttribute objAttr = new ObjAttribute("newcol2");
        objAttr.setDbAttributePath(column.getName());
        objEntity.addAttribute(objAttr);

        // check that is was merged
        assertTokensAndExecute(0, 0);

        // set not null
        column.setMandatory(true);

        // merge to db
        assertTokensAndExecute(2, 0);

        // check that is was merged
        assertTokensAndExecute(0, 0);

        // check values for null
        Expression qual = ExpressionFactory.matchExp(objAttr.getName(), DEFAULT_VALUE_STRING);
        ObjectSelect<Painting> query = ObjectSelect.query(Painting.class).where(qual);
        List<Painting> rows = query.select(context);
        assertEquals(nrows, rows.size());

        // clean up
        dbEntity.removeAttribute(column.getName());
        assertTokensAndExecute(1, 0);
        assertTokensAndExecute(0, 0);
    }

    @Override
    protected DataMapMerger.Builder merger() {
        return super.merger().valueForNullProvider(new DefaultValueForNullProvider() {

            @Override
            protected ParameterBinding get(DbEntity entity, DbAttribute column) {
                int type = column.getType();
                switch (type) {
                    case Types.VARCHAR:
                        return new ParameterBinding(DEFAULT_VALUE_STRING, type, -1);
                    default:
                        throw new AssertionFailedError("should not get here");
                }
            }

            @Override
            public boolean hasValueFor(DbEntity entity, DbAttribute column) {
                return true;
            }
        });
    }

}
