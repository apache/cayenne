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
 * @since 3.0
 */
public interface QuotingStrategy {

    String quoteString(String name);

    String quoteFullyQualifiedName(DbEntity entity);
}

class QuoteStrategy implements QuotingStrategy {

    private String endQuote;
    private String startQuote;

    public QuoteStrategy(String startQuote, String endQuote) {
        this.startQuote = startQuote;
        this.endQuote = endQuote;
    }

    public String quoteString(String name) {
        return startQuote + name + endQuote;
    }

    public String quoteFullyQualifiedName(DbEntity entity) {
        StringBuilder buf = new StringBuilder();
        if (entity.getSchema() != null) {
            buf.append(quoteString(entity.getSchema())).append(".");
        }
        buf.append(quoteString(entity.getName()));
        return buf.toString();
    }
}

class NoQuoteStrategy implements QuotingStrategy {

    public String quoteString(String name) {
        return name;
    }

    public String quoteFullyQualifiedName(DbEntity entity) {
        return entity.getFullyQualifiedName();
    }
}
