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

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Function;

import org.apache.cayenne.Persistent;
import org.apache.cayenne.cache.invalidation.db.E1;
import org.apache.cayenne.cache.invalidation.db.E2;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @since 4.0
 */
public class CacheGroupsHandlerTest {

    @Test
    public void canHandleE1() throws Exception {
        CacheGroupsHandler handler = new  CacheGroupsHandler();
        Function<Persistent, Collection<CacheGroupDescriptor>> function = handler.canHandle(E1.class);
        Collection<CacheGroupDescriptor> result = function.apply(null);

        assertEquals(2, result.size());

        String[] names = {"g1", "g2"};
        Collection<String> extractedNames = new ArrayList<>();
        for(CacheGroupDescriptor descriptor : result) {
            extractedNames.add(descriptor.getCacheGroupName());
        }
        assertArrayEquals(names, extractedNames.toArray());
    }

    @Test
    public void canHandleE2() throws Exception {
        CacheGroupsHandler handler = new  CacheGroupsHandler();
        Function<Persistent, Collection<CacheGroupDescriptor>> function = handler.canHandle(E2.class);
        Collection<CacheGroupDescriptor> result = function.apply(null);

        assertEquals(6, result.size());

        String[] names = {"g1", "g2", "g3", "g4", "g5", "g6"};
        Collection<String> extractedNames = new ArrayList<>();
        for(CacheGroupDescriptor descriptor : result) {
            extractedNames.add(descriptor.getCacheGroupName());
        }
        assertArrayEquals(names, extractedNames.toArray());
    }

}