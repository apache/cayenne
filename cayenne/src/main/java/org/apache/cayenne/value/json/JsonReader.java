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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @since 4.2
 */
class JsonReader extends AbstractJsonConsumer<Object> {

    private final Deque<Object> objects = new ArrayDeque<>(4);
    private final Deque<Object> names = new ArrayDeque<>(4);

    JsonReader(String json) {
        super(json);
    }

    @Override
    protected void onArrayStart() {
        objects.addLast(new ArrayList<>(4));
    }

    @Override
    protected void onObjectStart() {
        objects.addLast(new HashMap<>());
    }

    @Override
    protected void onArrayValue(JsonTokenizer.JsonToken token) {
        onArrayValue((Object)token);
    }

    @SuppressWarnings("unchecked")
    void onArrayValue(Object value) {
        Object array = objects.getLast();
        if(array instanceof List) {
            ((List<Object>) array).add(value);
        } else {
            throw new IllegalStateException("Expected List got " + array.getClass().getSimpleName());
        }
    }

    @Override
    protected void onObjectKey(JsonTokenizer.JsonToken token) {
        names.addLast(token);
    }

    @Override
    protected void onObjectValue(JsonTokenizer.JsonToken token) {
        onObjectValue(token, false);
    }

    @SuppressWarnings("unchecked")
    protected void onObjectValue(Object value, boolean updateState) {
        Object name = names.pollLast();
        Object map = objects.getLast();
        if(map instanceof Map) {
            ((Map<Object, Object>) map).put(name, value);
        } else {
            throw new IllegalStateException("Expected Map got " + map.getClass().getSimpleName());
        }
        if(updateState) {
            setState(State.OBJECT_KEY);
        }
    }

    @Override
    protected void onValue(JsonTokenizer.JsonToken token) {
        objects.addLast(token);
    }

    @Override
    protected void onArrayEnd() {
        Object value = objects.pollLast();
        onValue(value);
    }

    @Override
    protected void onObjectEnd() {
        Object value = objects.pollLast();
        onValue(value);
    }

    void onValue(Object value) {
        switch (currentState()) {
            case OBJECT_VALUE:
                onObjectValue(value, true);
                break;
            case ARRAY:
                onArrayValue(value);
                break;
            default:
                objects.addLast(value);
        }
    }

    @Override
    protected Object output() {
        if(objects.isEmpty()) {
            return null;
        }
        return objects.getFirst();
    }

}
