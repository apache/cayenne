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
package org.apache.cayenne.dba;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.Entity;

/**
 * @since 4.0 this is a top-level class.
 */
public class DefaultQuotingStrategy implements QuotingStrategy {

    private final String endQuote;
    private final String startQuote;

    public DefaultQuotingStrategy(String startQuote, String endQuote) {
        this.startQuote = startQuote;
        this.endQuote = endQuote;
    }

    /**
     * @deprecated since 4.0
     */
    @Override
    @Deprecated
    public String quoteString(String name) {
        return quotedIdentifier((DataMap) null, name);
    }

    @Override
    @Deprecated
    public String quoteFullyQualifiedName(DbEntity entity) {
        return quotedFullyQualifiedName(entity);
    }

    @Override
    public String quotedFullyQualifiedName(DbEntity entity) {
        return quotedIdentifier(entity.getDataMap(), entity.getCatalog(), entity.getSchema(), entity.getName());
    }

    @Override
    public String quotedName(DbAttribute attribute) {
        return quotedIdentifier(attribute.getEntity().getDataMap(), attribute.getName());
    }

    @Override
    public String quotedSourceName(DbJoin join) {
        DataMap dataMap = join.getSource().getEntity().getDataMap();
        return quotedIdentifier(dataMap, join.getSourceName());
    }

    @Override
    public String quotedTargetName(DbJoin join) {
        DataMap dataMap = join.getTarget().getEntity().getDataMap();
        return quotedIdentifier(dataMap, join.getTargetName());
    }

    @Override
    public String quotedIdentifier(Entity entity, String... identifierParts) {
        return quotedIdentifier(entity.getDataMap(), identifierParts);
    }

    /**
     * check in dataMap if we have to quote identifiers we add startQuote & endQuote around
     * each part from identifierParts and join all of them with '.' (dot)
     *
     */
    @Override
    public String quotedIdentifier(DataMap dataMap, String... identifierParts) {

        String startQuote;
        String endQuote;

        if (dataMap != null && dataMap.isQuotingSQLIdentifiers()) {
            startQuote = this.startQuote;
            endQuote = this.endQuote;
        } else {
            startQuote = "";
            endQuote = "";
        }

        StringBuilder buffer = new StringBuilder();

        for (String part : identifierParts) {

            if (part == null) {
                continue;
            }

            if (buffer.length() > 0) {
                buffer.append(".");
            }

            buffer.append(startQuote).append(part).append(endQuote);
        }

        return buffer.toString();
    }
}