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

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.profile.AbstractCase;
import org.apache.cayenne.profile.entity.Entity2;
import org.apache.cayenne.profile.entity.Entity3;

public class InsertWithRelationshipCase extends AbstractCase {

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        for (int i = 0; i < 500; i++) {
            Entity2 e = (Entity2) context.newObject(Entity2.class);
            e.setName("Name_" + i);
            
            Entity3 e31 = (Entity3) context.newObject(Entity3.class);
            e31.setName("E31_" + i);
            e31.setEntity2(e);

            Entity3 e32 = (Entity3) context.newObject(Entity3.class);
            e32.setName("E32_" + i);
            e32.setEntity2(e);
        }

        context.commitChanges();
    }
}
