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

package org.apache.cayenne.project.validator;

import java.util.Iterator;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.ProjectPath;
import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;
import org.apache.cayenne.util.Util;

/**
 * Validator for SQLTemplate queries.
 * 
 * @since 1.1
 */
public class SQLTemplateValidator extends TreeNodeValidator {

    @Override
    public void validateObject(ProjectPath treeNodePath, Validator validator) {
        SQLTemplate query = (SQLTemplate) treeNodePath.getObject();

        validateName(query, treeNodePath, validator);
        validateRoot(query, treeNodePath, validator);
        validateDefaultSQL(query, treeNodePath, validator);
    }

    protected void validateDefaultSQL(
            SQLTemplate query,
            ProjectPath path,
            Validator validator) {

        if (Util.isEmptyString(query.getDefaultTemplate())) {
            // see if there is at least one adapter-specific template...

            for (final String key : query.getTemplateKeys()) {
                if (!Util.isEmptyString(query.getCustomTemplate(key))) {
                    return;
                }
            }

            validator.registerWarning("Query has no default SQL template", path);
        }
    }

    protected void validateRoot(SQLTemplate query, ProjectPath path, Validator validator) {
        DataMap map = path.firstInstanceOf(DataMap.class);
        if (query.getRoot() == null && map != null) {
            validator.registerWarning("Query has no root", path);
        }
    }

    protected void validateName(Query query, ProjectPath path, Validator validator) {
        String name = query.getName();

        // Must have name
        if (Util.isEmptyString(name)) {
            validator.registerError("Unnamed Query.", path);
            return;
        }

        DataMap map = (DataMap) path.getObjectParent();
        if (map == null) {
            return;
        }

        // check for duplicate names in the parent context
        for (final Query otherQuery : map.getQueries()) {
            if (otherQuery == query) {
                continue;
            }

            if (name.equals(otherQuery.getName())) {
                validator.registerError("Duplicate Query name: " + name + ".", path);
                break;
            }
        }
    }
}
