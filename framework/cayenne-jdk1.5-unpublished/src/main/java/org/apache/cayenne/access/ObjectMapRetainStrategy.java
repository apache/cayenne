/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.access;

import java.util.Map;

import org.apache.cayenne.Persistent;

/**
 * A strategy for retaining objects in {@link ObjectStore}. May be used
 * weak, soft or hard references. 
 * 
 * @since 3.1
 */
public interface ObjectMapRetainStrategy {
    
    static final String MAP_RETAIN_STRATEGY_PROPERTY = "org.apache.cayenne.context_object_retain_strategy";
    
    static final String WEAK_RETAIN_STRATEGY = "weak";
    static final String SOFT_RETAIN_STRATEGY = "soft";
    static final String HARD_RETAIN_STRATEGY = "hard";
    
    Map<Object, Persistent> createObjectMap();

}
