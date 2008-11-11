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

package org.apache.cayenne.unit.util;

import java.util.Map;

import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.query.SQLTemplate;

/**
 * Helper class to customize SQLTemplate queries used in test cases per adapter.
 * 
 */
public class SQLTemplateCustomizer {

    protected DbAdapter adapter;
    protected Map sqlMap;

    public SQLTemplateCustomizer(Map sqlMap) {
        this.sqlMap = sqlMap;
    }

    /**
     * Customizes SQLTemplate, injecting the template for the current adapter.
     */
    public void updateSQLTemplate(SQLTemplate query) {
        Map customSQL = (Map) sqlMap.get(query.getDefaultTemplate());
        if (customSQL != null) {
            String key = adapter.getClass().getName();
            String template = (String) customSQL.get(key);
            if (template != null) {
                query.setTemplate(key, template);
            }
        }
    }

    public SQLTemplate createSQLTemplate(Class root, String defaultTemplate) {
        SQLTemplate template = new SQLTemplate(root, defaultTemplate);
        updateSQLTemplate(template);
        return template;
    }

    public DbAdapter getAdapter() {
        return adapter;
    }

    public void setAdapter(DbAdapter adapter) {
        this.adapter = adapter;
    }
}
