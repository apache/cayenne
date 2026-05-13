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
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.query.ObjectSelect;
import org.apache.cayenne.testdo.legacy_datetime.DateTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.CayenneTestsEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ASTFunctionCallDateIT {

    @RegisterExtension
    static final CayenneTestsEnv env = CayenneTestsEnv.forProject(CayenneProjects.LEGACY_DATE_TIME_PROJECT);

    private UnitDbAdapter unitDbAdapter;

    @BeforeEach
    public void createDataSet() throws Exception {
        unitDbAdapter = env.getInstance(UnitDbAdapter.class);
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.MILLISECOND, 0);

        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DateTestEntity o1 = env.context().newObject(DateTestEntity.class);
        cal.set(year - 1, month, day, 0, 0, 0);
        o1.setDateColumn(cal.getTime());
        cal.set(year, month, day, 0, 0, 0);
        o1.setTimeColumn(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, day - 1);
        o1.setTimestampColumn(cal.getTime());

        DateTestEntity o2 = env.context().newObject(DateTestEntity.class);
        cal.set(year + 1, month, day, 0, 0, 0);
        o2.setDateColumn(cal.getTime());
        cal.set(year, month, day, 23, 59, 59);
        o2.setTimeColumn(cal.getTime());
        cal.set(Calendar.DAY_OF_MONTH, day + 1);
        o2.setTimestampColumn(cal.getTime());

        env.context().commitChanges();
    }

    @Test
    public void currentDate() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("dateColumn", new ASTCurrentDate());
        DateTestEntity res1 = ObjectSelect.query(DateTestEntity.class, exp).selectOne(env.context());
        assertNotNull(res1);

        Expression exp2 = ExpressionFactory.lessExp("dateColumn", new ASTCurrentDate());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(env.context());
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void currentTime() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("timeColumn", new ASTCurrentTime());
        List<DateTestEntity> res = ObjectSelect.query(DateTestEntity.class, exp).select(env.context());
        if(!unitDbAdapter.supportsTimeSqlType()) {
            // check only that query is executed without error
            // result will be invalid most likely as DB doesn't support TIME data type
            return;
        }
        assertEquals(1, res.size());
        DateTestEntity res1 = res.get(0);

        Expression exp2 = ExpressionFactory.lessExp("timeColumn", new ASTCurrentTime());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(env.context());
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void currentTimestamp() throws Exception {
        Expression exp = ExpressionFactory.greaterOrEqualExp("timestampColumn", new ASTCurrentTimestamp());
        DateTestEntity res1 = ObjectSelect.query(DateTestEntity.class, exp).selectOne(env.context());
        assertNotNull(res1);

        Expression exp2 = ExpressionFactory.lessExp("timestampColumn", new ASTCurrentTimestamp());
        DateTestEntity res2 = ObjectSelect.query(DateTestEntity.class, exp2).selectOne(env.context());
        assertNotNull(res2);

        assertNotEquals(res1, res2);
    }

    @Test
    public void aSTCurrentDateParse() {
        Expression exp = ExpressionFactory.exp("dateColumn > currentDate()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(env.context());
        assertNotNull(res);
    }

    @Test
    public void aSTCurrentTimeParse() {
        Expression exp = ExpressionFactory.exp("timeColumn > currentTime()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(env.context());
        if(!unitDbAdapter.supportsTimeSqlType()) {
            return;
        }
        assertNotNull(res);
    }

    @Test
    public void aSTCurrentTimestampParse() {
        Expression exp = ExpressionFactory.exp("timestampColumn > now()");
        DateTestEntity res = ObjectSelect.query(DateTestEntity.class, exp).selectOne(env.context());
        assertNotNull(res);
    }
}
