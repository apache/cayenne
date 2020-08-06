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

package org.apache.cayenne.cache.invalidation;

import org.apache.cayenne.Persistent;

import java.util.Collection;
import java.util.function.Function;

/**
 * @since 4.0
 * @deprecated since 4.1 plain Function&gt;Persistent, Collection&gt;CacheGroupDescriptor>> can be used.
 */
@Deprecated
public interface InvalidationFunction extends Function<Persistent, Collection<CacheGroupDescriptor>> {

    /**
     * @return collection of cache groups to invalidate for given object
     */
    Collection<CacheGroupDescriptor> apply(Persistent persistent);

}