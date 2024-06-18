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

import java.util.Objects;

/**
 * Simple utils to process JSON.
 *
 * @since 4.2
 * @see org.apache.cayenne.value.Json
 */
public final class JsonUtils {

    /**
     * Cleanup and reformat any valid JSON string.
     * Generally this methods just removes unnecessary whitespaces in the document.
     *
     * @param json valid JSON document
     * @return normalized JSON
     */
    public static String normalize(String json) {
        return new JsonFormatter(json).process();
    }

    /**
     * <p>
     * Method that compares two JSON documents.
     * <br>
     * This methods will parse documents so it will ignores object keys ordering and whitespaces.
     * </p>
     * <b>NOTE</b> this method doesn't parse numbers so same numbers in different format will be different.
     *
     * @param json1 first value
     * @param json2 second value
     * @return true if documents are equal
     */
    public static boolean compare(String json1, String json2) {
        Object object1 = new JsonReader(json1).process();
        Object object2 = new JsonReader(json2).process();
        return Objects.equals(object1, object2);
    }

    private JsonUtils() {
    }

}
