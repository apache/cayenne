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

package org.apache.cayenne.access.sqlbuilder;

import org.apache.cayenne.dba.QuotingStrategy;

/**
 * @since 4.2
 */
public class DefaultSQLAppendable implements SQLAppendable {

    protected final StringBuilder builder;
    protected final SQLGenerationContext context;
    protected final QuotingStrategy quotingStrategy;

    public DefaultSQLAppendable(SQLGenerationContext context) {
        this.builder = new StringBuilder();
        this.context = context;
        this.quotingStrategy = context == null ? null : context.getAdapter().getQuotingStrategy();
    }

    @Override
    public SQLAppendable append(String str) {
        builder.append(str);
        return this;
    }

    @Override
    public SQLAppendable append(char c) {
        builder.append(c);
        return this;
    }

    @Override
    public SQLAppendable append(int c) {
        builder.append(c);
        return this;
    }

    @Override
    public SQLAppendable appendQuoted(String str) {
        if (quotingStrategy == null) {
            builder.append(str);
        } else {
            quotingStrategy.quotedIdentifier(context.getRootDbEntity(), str, builder);
        }
        return this;
    }

    @Override
    public SQLGenerationContext getContext() {
        return context;
    }

    @Override
    public String getSql() {
        return builder.toString();
    }

    @Override
    public String toString() {
        return getSql();
    }

    /**
     * @deprecated unused
     */
    @Deprecated(since = "5.0", forRemoval = true)
    public StringBuilder unwrap() {
        return builder;
    }
}
