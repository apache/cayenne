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

package org.apache.cayenne.access;

import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.query.EJBQLQuery;
import org.apache.cayenne.testdo.date_time.DateTestEntity;
import org.apache.cayenne.unit.UnitDbAdapter;
import org.apache.cayenne.unit.di.runtime.CayenneProjects;
import org.apache.cayenne.unit.di.runtime.RuntimeCase;
import org.apache.cayenne.unit.di.runtime.UseCayenneRuntime;
import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@UseCayenneRuntime(CayenneProjects.DATE_TIME_PROJECT)
public class DataContextEJBQLDateTimeFunctionalExpressionsIT extends RuntimeCase {

    @Inject
    private ObjectContext context;

    @Inject
    private UnitDbAdapter unitDbAdapter;

    @Test
    public void testCURRENT_DATE() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year - 3, 1, 1);
        o1.setDateColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year + 3, 1, 1);
        o2.setDateColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.dateColumn > CURRENT_DATE");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o2));
    }

    @Test
    public void testCURRENT_TIME() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int day = cal.get(Calendar.DAY_OF_MONTH);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, month, day, 0, 0, 0);
        o1.setTimeColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, month, day, 23, 59, 59);
        o2.setTimeColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timeColumn < CURRENT_TIME");
        List<?> objects = context.performQuery(query);
        if(!unitDbAdapter.supportsTimeSqlType()) {
            // check only that query is executed without error
            // result will be invalid most likely as DB doesn't support TIME data type
            return;
        }
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }

    @Test
    public void testCURRENT_TIMESTAMP() {

        Calendar cal = Calendar.getInstance();
        int year = cal.get(Calendar.YEAR);
        int month = cal.get(Calendar.MONTH);
        int date = cal.get(Calendar.DATE);

        DateTestEntity o1 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 0, 0, 0);
        o1.setTimestampColumn(cal.getTime());

        DateTestEntity o2 = context.newObject(DateTestEntity.class);
        cal.set(year, month, date, 23, 59, 59);
        o2.setTimestampColumn(cal.getTime());

        context.commitChanges();

        EJBQLQuery query = new EJBQLQuery(
                "SELECT d FROM DateTestEntity d WHERE d.timestampColumn < CURRENT_TIMESTAMP");
        List<?> objects = context.performQuery(query);
        assertEquals(1, objects.size());
        assertTrue(objects.contains(o1));
    }
}
