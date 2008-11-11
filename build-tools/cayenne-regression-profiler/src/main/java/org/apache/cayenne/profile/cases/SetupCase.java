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

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataDomain;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.access.DbGenerator;
import org.apache.cayenne.profile.AbstractCase;

/**
 * Performs initial database setup before running other tests. Essentially drops and
 * recreates the database.
 * 
 */
public class SetupCase extends AbstractCase {

    protected void doRequest(
            DataContext context,
            HttpServletRequest request,
            HttpServletResponse response) {

        DataDomain domain = context.getParentDataDomain();
        DataNode node = domain.getNode("regression-profile");
        DbGenerator generator = new DbGenerator(node.getAdapter(), domain
                .getMap("regression-profile"));

        generator.setShouldCreateFKConstraints(true);
        generator.setShouldCreatePKSupport(true);
        generator.setShouldCreateTables(true);
        generator.setShouldDropPKSupport(true);
        generator.setShouldDropTables(true);
        try {
            generator.runGenerator(node.getDataSource());
        }
        catch (Exception e) {
            throw new CayenneRuntimeException("Error generating schema", e);
        }
    }
}
