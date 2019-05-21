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

/**
 * @since 4.2
 */
public class StringBuilderAppendable implements QuotingAppendable {

    protected final StringBuilder builder;

    public StringBuilderAppendable() {
        this.builder = new StringBuilder();
    }

    @Override
    public QuotingAppendable append(CharSequence csq) {
        builder.append(csq);
        return this;
    }

    @Override
    public QuotingAppendable append(CharSequence csq, int start, int end) {
        builder.append(csq, start, end);
        return this;
    }

    @Override
    public QuotingAppendable append(char c) {
        builder.append(c);
        return this;
    }

    @Override
    public QuotingAppendable append(int c) {
        builder.append(c);
        return this;
    }

    @Override
    public QuotingAppendable appendQuoted(CharSequence csq) {
        builder.append(csq);
        return this;
    }

    @Override
    public SQLGenerationContext getContext() {
        return null;
    }

    @Override
    public String toString() {
        return builder.toString();
    }

    public StringBuilder unwrap() {
        return builder;
    }
}
