/*
 *  Copyright 2006 The Apache Software Foundation
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package org.apache.cayenne.profile.cases;

import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.profile.AbstractCase;
import org.apache.cayenne.profile.entity.Entity1;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.query.SelectQuery;

/**
 * Does a random insert/delete/update based on the current object count.
 * 
 */
public class RandomOperationCase extends AbstractCase {

    static final int MEDIAN_COUNT = 500;
    static final int RANGE = 50;

    protected Random r = new Random(System.currentTimeMillis());
    protected int minObjects = MEDIAN_COUNT - (RANGE / 2);
    protected int maxObjects = MEDIAN_COUNT + (RANGE / 2);

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (r.nextBoolean()) {
            doSelect(context);
            return;
        }

        // do update...
        String table = context
                .getEntityResolver()
                .lookupDbEntity(Entity1.class)
                .getName();
        SQLTemplate count = new SQLTemplate(
                Entity1.class,
                "SELECT #result('count(*)' 'int' 'C') FROM " + table);
        count.setFetchingDataRows(true);
        Map row = (Map) context.performQuery(count).get(0);
        int c = ((Number) row.get("C")).intValue();

        if (c < minObjects) {
            doInsert(context);
        }
        else if (c > maxObjects) {
            doDelete(context);
        }
        else {
            switch (r.nextInt(3)) {
                case 0:
                    doInsert(context);
                    break;
                case 1:
                    doUpdate(context);
                    break;
                case 2:
                    doDelete(context);
                    break;
            }
        }

        context.commitChanges();
    }

    protected void doInsert(DataContext context) {
        Entity1 e = (Entity1) context.newObject(Entity1.class);
        e.setName("X" + System.currentTimeMillis());
    }

    protected void doUpdate(DataContext context) {
        Entity1 e = getRandomObject(context);
        if (e != null) {
            e.setName("Y" + System.currentTimeMillis());
        }
    }

    protected void doDelete(DataContext context) {
        Entity1 e = getRandomObject(context);
        if (e != null) {
            context.deleteObject(e);
        }
    }

    protected void doSelect(DataContext context) {
        SelectQuery q = new SelectQuery(Entity1.class);
        q.setFetchLimit(300);
        context.performQuery(q);
    }

    protected Entity1 getRandomObject(DataContext context) {
        SelectQuery q = new SelectQuery(Entity1.class);
        q.setPageSize(3);

        for (int i = 0; i < 20; i++) {
            List allObjects = context.performQuery(q);
            if (allObjects.size() > 0) {
                int x = r.nextInt(allObjects.size());

                try {
                    return (Entity1) allObjects.get(x);
                }
                catch (CayenneRuntimeException e) {
                    // this can happen due to concurrency isses - other threads may have
                    // deleted this page
                }
            }
        }

        return null;
    }
}
