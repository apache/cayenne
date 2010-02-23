/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.project2.validation;

import org.apache.cayenne.map.EntityResolver;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.project2.validation.EJBQLStatementValidator.PositionException;
import org.apache.cayenne.query.EJBQLQuery;

class EJBQLQueryValidator {

    void validate(Object object, ValidationVisitor validationVisitor) {
        EJBQLQuery query = (EJBQLQuery) object;

        ProjectPath path = new ProjectPath(new Object[] {
                query.getDataMap().getDataChannelDescriptor(), query.getDataMap(), query
        });

        PositionException message = (new EJBQLStatementValidator()).validateEJBQL(
                query,
                new EntityResolver(query
                        .getDataMap()
                        .getDataChannelDescriptor()
                        .getDataMaps()));

        if (message != null) {
            validationVisitor.registerWarning("EJBQL query "
                    + query.getName()
                    + " has error.", path);
        }
    }
}
