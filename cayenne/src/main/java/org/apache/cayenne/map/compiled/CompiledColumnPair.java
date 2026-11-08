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
 * A pair of columns joined by a {@link CompiledForeignKey}: a foreign key column of the source table, and the column
 * it references in the target table. In a {@link CompiledJoin} the pair is oriented in the direction of the
 * join, so for the reverse join the source is the referenced column.
 *
 * @since 6.0
 */
public record CompiledColumnPair(CompiledColumn source, CompiledColumn target) {

    CompiledColumnPair reversed() {
        return new CompiledColumnPair(target, source);
    }

    @Override
    public String toString() {
        return source + " = " + target;
    }
}
