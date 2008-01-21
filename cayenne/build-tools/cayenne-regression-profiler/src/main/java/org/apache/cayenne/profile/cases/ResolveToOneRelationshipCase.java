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

import java.util.Iterator;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.Fault;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.profile.AbstractCase;
import org.apache.cayenne.profile.entity.Entity3;
import org.apache.cayenne.query.SelectQuery;

public class ResolveToOneRelationshipCase extends AbstractCase {

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        SelectQuery q = new SelectQuery(Entity3.class, Expression
                .fromString("name like '%_111%'"));
        List results = context.performQuery(q);
        assertEquals(200, results.size());

        Iterator it = results.iterator();
        while (it.hasNext()) {
            Entity3 e3 = (Entity3) it.next();
            assertTrue(e3.readPropertyDirectly("entity2") instanceof Fault);
            e3.getEntity2().getName();
            assertFalse(e3.readPropertyDirectly("entity2") instanceof Fault);
        }
    }
}
