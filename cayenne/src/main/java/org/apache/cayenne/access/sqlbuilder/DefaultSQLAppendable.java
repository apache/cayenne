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

    final StringBuilder builder;
    private final SQLGenerationContext context;
    private final QuotingStrategy quotingStrategy;
    private boolean suppressNextSeparator;

    public DefaultSQLAppendable(SQLGenerationContext context) {
        this.builder = new StringBuilder();
        this.context = context;
        this.quotingStrategy = resolveQuotes(context);
    }

    private static QuotingStrategy resolveQuotes(SQLGenerationContext context) {
        return context == null
                ? QuotingStrategy.NONE
                : context.getAdapter().getQuotingStrategy(context.getRootDbEntity());
    }

    @Override
    public SQLAppendable append(String str) {
        suppressNextSeparator = false;
        builder.append(str);
        return this;
    }

    @Override
    public SQLAppendable append(char c) {
        suppressNextSeparator = false;
        builder.append(c);
        return this;
    }

    @Override
    public SQLAppendable append(int c) {
        suppressNextSeparator = false;
        builder.append(c);
        return this;
    }

    @Override
    public SQLAppendable appendQuoted(String str) {
        suppressNextSeparator = false;
        quotingStrategy.appendStart(builder);
        builder.append(str);
        quotingStrategy.appendEnd(builder);
        return this;
    }

    @Override
    public SQLAppendable appendTokenSeparator() {
        if (suppressNextSeparator) {
            suppressNextSeparator = false;
        } else {
            builder.append(' ');
        }
        return this;
    }

    @Override
    public SQLAppendable suppressNextTokenSeparator() {
        suppressNextSeparator = true;
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
}
