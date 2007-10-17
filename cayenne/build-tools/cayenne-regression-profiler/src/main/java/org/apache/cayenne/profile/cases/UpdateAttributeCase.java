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

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.profile.AbstractCase;
import org.apache.cayenne.profile.entity.Entity1;

public class UpdateAttributeCase extends AbstractCase {

    private static volatile int suffix = 1;

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        

        for (int i = 600; i < 1100; i++) {
            Entity1 o = (Entity1) DataObjectUtils.objectForPK(context, Entity1.class, i);

            assertNotNull("No object for id: " + i, o);

            o.setDescription("x_" + suffix++);
        }

        context.commitChanges();
    }
}
