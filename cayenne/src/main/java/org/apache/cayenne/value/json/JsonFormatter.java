/*
 * Licensed to the Apache Software Foundation (ASF) under one
 *    or more contributor license agreements.  See the NOTICE file
 *    distributed with this work for additional information
 *    regarding copyright ownership.  The ASF licenses this file
 *    to you under the Apache License, Version 2.0 (the
 *    "License"); you may not use this file except in compliance
 *    with the License.  You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing,
 *    software distributed under the License is distributed on an
 *    "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *    KIND, either express or implied.  See the License for the
 *    specific language governing permissions and limitations
 *    under the License.
 */

package org.apache.cayenne.value.json;

/**
 * @since 4.2
 */
class JsonFormatter extends AbstractJsonConsumer<String> {

    private final StringBuilder builder = new StringBuilder();

    JsonFormatter(String json) {
        super(json);
    }

    @Override
    protected void onArrayStart() {
        builder.append('[');
    }

    @Override
    protected void onArrayEnd() {
        if(builder.charAt(builder.length() - 1) == ' ') {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append(']');
        if (State.OBJECT_VALUE.equals(currentState())) {
            setState(State.OBJECT_KEY);
            builder.append(", ");
        }
    }

    @Override
    protected void onObjectStart() {
        builder.append('{');
    }

    @Override
    protected void onObjectEnd() {
        if(builder.charAt(builder.length() - 1) == ' ') {
            builder.delete(builder.length() - 2, builder.length());
        }
        builder.append('}');
        if (State.ARRAY.equals(currentState())) {
            builder.append(", ");
        }
    }

    @Override
    protected void onArrayValue(JsonTokenizer.JsonToken token) {
        appendToken(token);
        builder.append(", ");
    }

    @Override
    protected void onObjectKey(JsonTokenizer.JsonToken token) {
        appendToken(token);
        builder.append(": ");
    }

    @Override
    protected void onObjectValue(JsonTokenizer.JsonToken token) {
        appendToken(token);
        builder.append(", ");
    }

    @Override
    protected void onValue(JsonTokenizer.JsonToken token) {
        appendToken(token);
    }

    @Override
    protected String output() {
        return builder.toString();
    }

    private void appendToken(JsonTokenizer.JsonToken token) {
        if(token.type == JsonTokenizer.TokenType.STRING) {
            builder.append('"');
        }
        builder.append(token.getData(), token.from, token.to - token.from);
        if(token.type == JsonTokenizer.TokenType.STRING) {
            builder.append('"');
        }
    }
}
