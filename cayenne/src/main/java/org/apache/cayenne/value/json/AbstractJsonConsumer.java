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
abstract class AbstractJsonConsumer<T> {

    private final JsonTokenizer tokenizer;
    private State[] states = new State[4];
    private int currentState = 0;

    AbstractJsonConsumer(String json) {
        states[currentState] = State.NONE;
        tokenizer = new JsonTokenizer(json);
    }

    protected abstract void onArrayStart();

    protected abstract void onArrayEnd();

    protected abstract void onObjectStart();

    protected abstract void onObjectEnd();

    protected abstract void onArrayValue(JsonTokenizer.JsonToken token);

    protected abstract void onObjectKey(JsonTokenizer.JsonToken token);

    protected abstract void onObjectValue(JsonTokenizer.JsonToken token);

    protected abstract void onValue(JsonTokenizer.JsonToken token);

    protected abstract T output();

    T process() {
        JsonTokenizer.JsonToken token = tokenizer.nextToken();
        if (token.getType() == JsonTokenizer.TokenType.NONE) {
            throw new MalformedJsonException("Unexpected EOF");
        }
        do {
            switch (token.getType()) {
                case ARRAY_START:
                    onArrayStart();
                    pushState(State.ARRAY);
                    break;

                case OBJECT_START:
                    onObjectStart();
                    pushState(State.OBJECT_KEY);
                    break;

                case ARRAY_END:
                    popState();
                    onArrayEnd();
                    break;

                case OBJECT_END:
                    popState();
                    onObjectEnd();
                    break;

                default:
                    processValue(token);
                    break;
            }
        } while ((token = tokenizer.nextToken()).getType() != JsonTokenizer.TokenType.NONE);
        return output();
    }

    private void processValue(JsonTokenizer.JsonToken token) {
        switch (states[currentState]) {
            case OBJECT_KEY:
                setState(State.OBJECT_VALUE);
                onObjectKey(token);
                break;
            case OBJECT_VALUE:
                setState(State.OBJECT_KEY);
                onObjectValue(token);
                break;
            case ARRAY:
                onArrayValue(token);
                break;
            default:
                onValue(token);
        }
    }

    protected void pushState(State state) {
        currentState++;
        if(currentState >= states.length) {
            State[] newStates = new State[states.length << 2];
            System.arraycopy(states, 0, newStates, 0, states.length);
            states = newStates;
        }
        states[currentState] = state;
    }

    protected void setState(State state) {
        states[currentState] = state;
    }

    protected void popState() {
        states[currentState--] = null;
    }

    protected State currentState() {
        return states[currentState];
    }

    protected enum State {
        NONE,
        ARRAY,
        OBJECT_KEY,
        OBJECT_VALUE
    }
}
