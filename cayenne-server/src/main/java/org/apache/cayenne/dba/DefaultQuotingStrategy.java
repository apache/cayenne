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
package org.apache.cayenne.dba;

import java.io.IOException;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.map.DbJoin;
import org.apache.cayenne.map.Procedure;

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

    /**
     * @since 4.2
     */
    @Override
    public void quotedIdentifier(DataMap dataMap, CharSequence identifier, Appendable appender) {
        if (identifier == null) {
            return;
        }

        boolean quoting = dataMap != null && dataMap.isQuotingSQLIdentifiers();
        try {
            if (quoting) {
                appender.append(startQuote).append(identifier).append(endQuote);
            } else {
                appender.append(identifier);
            }
        } catch (IOException ex) {
            throw new CayenneRuntimeException("Failed to append quoted identifier", ex);
        }
    }

    @Override
    public String quotedIdentifier(DataMap dataMap, String... identifierParts) {
        boolean quoting = dataMap != null && dataMap.isQuotingSQLIdentifiers();
        StringBuilder buffer = new StringBuilder();

        for (String part : identifierParts) {
            if (part == null) {
                continue;
            }
            if (buffer.length() > 0) {
                buffer.append('.');
            }
            if(quoting) {
                buffer.append(startQuote).append(part).append(endQuote);
            } else {
                buffer.append(part);
            }
        }

        return buffer.toString();
    }
}