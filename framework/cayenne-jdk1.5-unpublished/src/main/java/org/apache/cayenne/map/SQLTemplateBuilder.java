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

package org.apache.cayenne.map;

import java.util.Map;

import org.apache.cayenne.query.Query;
import org.apache.cayenne.query.SQLTemplate;

/**
 * QueryBuilder for the SQLTemplates.
 * 
 * @since 1.1
 */
class SQLTemplateBuilder extends QueryLoader {

    /**
     * Builds a SQLTemplate query.
     */
    @Override
    public Query getQuery() {

        SQLTemplate template = new SQLTemplate();
        Object root = getRoot();

        if (root != null) {
            template.setRoot(root);
        }

        template.setName(name);
        template.setDataMap(dataMap);
        template.initWithProperties(properties);

        // init SQL
        template.setDefaultTemplate(sql);
        if (adapterSql != null) {
            for (Map.Entry<String, String> entry : adapterSql.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();
                if (key != null && value != null) {
                    template.setTemplate(key, value);
                }
            }
        }

        return template;
    }
}
