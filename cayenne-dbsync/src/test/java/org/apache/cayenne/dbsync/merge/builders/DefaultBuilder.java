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
package org.apache.cayenne.dbsync.merge.builders;

import org.apache.cayenne.datafactory.DataFactory;

public abstract class DefaultBuilder<T> implements Builder<T> {

    protected final DataFactory dataFactory;
    protected final T obj;

    protected DefaultBuilder(T obj) {
        this.dataFactory = new DataFactory();
        this.obj = obj;
    }

    public String getRandomJavaName() {
        int count = dataFactory.getNumberBetween(1, 5);
        StringBuilder res = new StringBuilder();
        for (int i = 0; i < count; i++) {
            res.append(capitalized(dataFactory.getRandomWord()));
        }

        return uncapitalized(res.toString());
    }

    @Override
    public T build() {
        return obj;
    }

    @Override
    public T random() {
        return build();
    }

    protected static String uncapitalized(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        char c = Character.toLowerCase(s.charAt(0));
        return (s.length() == 1) ? Character.toString(c) : c + s.substring(1);
    }

    protected static String capitalized(String s) {
        if (s == null || s.isEmpty()) {
            return s;
        }

        char c = Character.toUpperCase(s.charAt(0));
        return (s.length() == 1) ? Character.toString(c) : c + s.substring(1);
    }
}
