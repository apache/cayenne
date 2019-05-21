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
package org.apache.cayenne.cache.invalidation.db;

import org.apache.cayenne.cache.invalidation.CacheGroup;
import org.apache.cayenne.cache.invalidation.CacheGroups;
import org.apache.cayenne.cache.invalidation.db.auto._E2;


@CacheGroups(
        value = {"g1", "g2"},
        groups = {
            @CacheGroup("g3"),
            @CacheGroup(value = "g4", keyType = String.class, valueType = Object.class),
            @CacheGroup(value = "g5", keyType = Integer.class, valueType = Object.class),
        }
)
@CacheGroup("g6")
public class E2 extends _E2 {

    private static final long serialVersionUID = 1L; 

}
