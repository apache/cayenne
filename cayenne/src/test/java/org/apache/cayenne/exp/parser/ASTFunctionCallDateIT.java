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

package org.apache.cayenne.exp.parser;

import java.util.Calendar;
import java.util.List;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.test.jdbc.DBHelper;
import org.apache.cayenne.testdo.date_time.DateTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @since 4.0
 */
@UseCayenneRuntime(CayenneProjects.DATE_TIME_PROJECT)
public class ASTFunctionCallDateIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private DBHelper dbHelper;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Before
    public void createDataSet() throws Exception {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year - 1, month, day, 0, 0, 0);
        o1.setDateColumn(cal.getTime());
        cal.set(year, month, day, 0, 0, 0);
        o1.setTimeColumn(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, day - 1);
        o1.setTimestampColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year + 1, month, day, 0, 0, 0);
        o2.setDateColumn(cal.getTime());
        cal.set(year, month, day, 23, 59, 59);
        o2.setTimeColumn(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, day + 1);
        o2.setTimestampColumn(cal.getTime());

        context.commitChanges();
    }

    @Test
    public void testCurrentDate() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("dateColumn", new ASTCurrentDate());
        DateTestEntity res1 = ObjectSelect.query(DateTestEntity.class, exp).selectOne(context);
        assertNotNull(res1);

        Expression exp2 = ExpressionFactory.lessExp("dateColumn", new ASTCurrentDate());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(context);
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void testCurrentTime() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("timeColumn", new ASTCurrentTime());
        List<DateTestEntity> res = ObjectSelect.query(DateTestEntity.class, exp).select(context);
        if(!unitDbAdapter.supportsTimeSqlType()) {
            // check only that query is executed without error
            // result will be invalid most likely as DB doesn't support TIME data type
            return;
        }
        assertEquals(1, res.size());
        DateTestEntity res1 = res.get(0);

        Expression exp2 = ExpressionFactory.lessExp("timeColumn", new ASTCurrentTime());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(context);
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void testCurrentTimestamp() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("timestampColumn", new ASTCurrentTimestamp());
        DateTestEntity res1 = ObjectSelect.query(DateTestEntity.class, exp).selectOne(context);
        assertNotNull(res1);

        Expression exp2 = ExpressionFactory.lessExp("timestampColumn", new ASTCurrentTimestamp());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(context);
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void testASTCurrentDateParse() {
        Expression exp = ExpressionFactory.exp("dateColumn > currentDate()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(context);
        assertNotNull(res);
    }

    @Test
    public void testASTCurrentTimeParse() {
        Expression exp = ExpressionFactory.exp("timeColumn > currentTime()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(context);
        if(!unitDbAdapter.supportsTimeSqlType()) {
            return;
        }
        assertNotNull(res);
    }

    @Test
    public void testASTCurrentTimestampParse() {
        Expression exp = ExpressionFactory.exp("timestampColumn > now()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(context);
        assertNotNull(res);
    }
}
