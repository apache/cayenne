/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/

package org.apache.cayenne.access.translator.select;

import org.apache.cayenne.access.sqlbuilder.sqltree.Node;
import org.apache.cayenne.exp.path.CayennePath;
import org.apache.cayenne.map.DbAttribute;

import static org.apache.cayenne.access.sqlbuilder.SQLBuilder.table;

/**
 * @since 4.2
 */
abstract class BaseColumnExtractor implements ColumnExtractor {

    protected final TranslatorContext context;

    BaseColumnExtractor(TranslatorContext context) {
        this.context = context;
    }

    protected void addDbAttribute(CayennePath prefix, CayennePath labelPrefix, DbAttribute dba) {
        String alias = context.getTableTree().aliasForPath(prefix);
        CayennePath dataRowKey = labelPrefix.dot(dba.getName());
        Node columnNode = table(alias).column(dba).build();
        context.addResultNode(columnNode, dataRowKey).setDbAttribute(dba);
    }
}
