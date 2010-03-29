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
package org.apache.cayenne.cache;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.TestCase;

import org.apache.cayenne.cache.OSQueryCache.RefreshSpecification;
import org.apache.cayenne.query.MockQueryMetadata;
import org.apache.cayenne.query.QueryMetadata;

import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

public class OSQueryCacheTest extends TestCase {

    public void testDefaults() {
        OSQueryCache cache = new OSQueryCache();

        assertNull(cache.refreshSpecifications);
        assertNull(cache.defaultRefreshSpecification.cronExpression);
        assertEquals(
                CacheEntry.INDEFINITE_EXPIRY,
                cache.defaultRefreshSpecification.refreshPeriod);
    }

    public void testDefaultOverrides() {

        Properties props = new Properties();
        props.put(OSQueryCache.DEFAULT_REFRESH_KEY, "15");
        props.put(OSQueryCache.DEFAULT_CRON_KEY, "9 * * * * *");
        OSQueryCache cache = new OSQueryCache(new GeneralCacheAdministrator(), props);

        assertNull(cache.refreshSpecifications);
        assertEquals("9 * * * * *", cache.defaultRefreshSpecification.cronExpression);
        assertEquals(15, cache.defaultRefreshSpecification.refreshPeriod);
    }

    public void testQueryOverrides() {

        Properties props = new Properties();
        props.put(OSQueryCache.GROUP_PREFIX + "ABC" + OSQueryCache.REFRESH_SUFFIX, "25");
        props.put(
                OSQueryCache.GROUP_PREFIX + "ABC" + OSQueryCache.CRON_SUFFIX,
                "12 * * * * *");
        props.put(OSQueryCache.GROUP_PREFIX + "XYZ" + OSQueryCache.REFRESH_SUFFIX, "35");
        props.put(
                OSQueryCache.GROUP_PREFIX + "XYZ" + OSQueryCache.CRON_SUFFIX,
                "24 * * * * *");

        OSQueryCache cache = new OSQueryCache(new GeneralCacheAdministrator(), props);

        assertNotNull(cache.refreshSpecifications);
        assertEquals(2, cache.refreshSpecifications.size());

        RefreshSpecification abc = cache.refreshSpecifications.get("ABC");
        assertNotNull(abc);
        assertEquals("12 * * * * *", abc.cronExpression);
        assertEquals(25, abc.refreshPeriod);

        RefreshSpecification xyz = cache.refreshSpecifications.get("XYZ");
        assertNotNull(xyz);
        assertEquals("24 * * * * *", xyz.cronExpression);
        assertEquals(35, xyz.refreshPeriod);
    }
    
    public void testGroupNames() {

        Properties props = new Properties();
        OSQueryCache c1 = new OSQueryCache(new GeneralCacheAdministrator(), props);
        assertTrue(c1.getGroupNames().isEmpty());

        props.put(OSQueryCache.GROUP_PREFIX + "ABC" + OSQueryCache.REFRESH_SUFFIX, "25");
        props.put(
                OSQueryCache.GROUP_PREFIX + "XYZ" + OSQueryCache.CRON_SUFFIX,
                "24 * * * * *");

        OSQueryCache c2 = new OSQueryCache(new GeneralCacheAdministrator(), props);

        assertEquals(2, c2.getGroupNames().size());

        assertTrue(c2.getGroupNames().contains("ABC"));
        assertTrue(c2.getGroupNames().contains("XYZ"));
    }

    public void testSize() {
        OSQueryCache cache = new OSQueryCache();

        List r1 = new ArrayList();
        QueryMetadata m1 = new MockQueryMetadata() {

            @Override
            public String getCacheKey() {
                return "a";
            }
        };
        cache.put(m1, r1);
        assertEquals(1, cache.size());
        
        List r2 = new ArrayList();
        QueryMetadata m2 = new MockQueryMetadata() {

            @Override
            public String getCacheKey() {
                return "b";
            }
        };
        cache.put(m2, r2);
        assertEquals(2, cache.size());
    }
}
