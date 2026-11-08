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
package org.apache.cayenne.map.compiled;

/**
 * The sequence the primary keys of a table are generated from: a named database sequence or sequence table, and
 * the number of keys fetched from it at a time.
 *
 * @param type the generator type, "ORACLE" for a database sequence
 * @param name the name of the sequence, or null
 * @param cacheSize  the number of keys to generate at once, or null for the default
 * @since 6.0
 */
public record CompiledKeySequence(String type, String name, Integer cacheSize) {

    public static final String ORACLE_TYPE = "ORACLE";
}
