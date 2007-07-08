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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.profile.AbstractCase;
import org.apache.cayenne.profile.entity.Entity2;
import org.apache.cayenne.profile.entity.Entity3;

public class MixedCommitCase extends AbstractCase {

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        for (int i = 600; i < 850; i++) {
            Entity2 o1 = (Entity2) DataObjectUtils.objectForPK(
                    context,
                    Entity2.class,
                    i * 2);
            Entity2 o2 = (Entity2) DataObjectUtils.objectForPK(
                    context,
                    Entity2.class,
                    i * 2 + 1);

            List e3s1 = o1.getEntity3s();
            assertEquals(2, e3s1.size());

            Entity3 e311 = (Entity3) e3s1.get(0);
            Entity3 e312 = (Entity3) e3s1.get(1);

            e311.setEntity2(o2);
            e312.setEntity2(null);
            context.deleteObject(e312);
        }

        context.commitChanges();
    }
}
