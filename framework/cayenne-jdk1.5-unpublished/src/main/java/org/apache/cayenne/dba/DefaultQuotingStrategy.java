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

import org.apache.cayenne.map.DbEntity;

/**
 * @since 3.2 this is a top-level class.
 */
class DefaultQuotingStrategy implements QuotingStrategy {

    private String endQuote;
    private String startQuote;

    DefaultQuotingStrategy(String startQuote, String endQuote) {
        this.startQuote = startQuote;
        this.endQuote = endQuote;
    }

    /**
     * @deprecated since 3.2
     */
    @Deprecated
    public String quoteString(String name) {
        return quotedIdentifier(name);
    }
    
    @Deprecated
    public String quoteFullyQualifiedName(DbEntity entity) {
       return quotedFullyQualifiedName(entity);
    }

    public String quotedFullyQualifiedName(DbEntity entity) {
        return quotedIdentifier(entity.getCatalog(), entity.getSchema(), entity.getName());
    }

    public String quotedIdentifier(String... fqnParts) {

        StringBuilder buffer = new StringBuilder();

        for (String part : fqnParts) {
            
            if(part == null) {
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