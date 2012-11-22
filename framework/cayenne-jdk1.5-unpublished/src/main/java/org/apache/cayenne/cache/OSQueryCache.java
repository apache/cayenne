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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.query.QueryMetadata;

import com.opensymphony.oscache.base.CacheEntry;
import com.opensymphony.oscache.base.NeedsRefreshException;
import com.opensymphony.oscache.general.GeneralCacheAdministrator;

/**
 * A {@link QueryCache} implementation based on OpenSymphony OSCache. Query cache
 * parameters are initialized from "/oscache.properties" file per <a
 * href="http://www.opensymphony.com/oscache/wiki/Configuration.html">OSCache</a>
 * documentation. In addition to the standard OSCache parameters, Cayenne provider allows
 * to setup global cache expiration parameters, and parameters matching the main query
 * cache group (i.e. the cache groups specified first). A sample oscache.properties may
 * look like this:
 * 
 * <pre>
 * # OSCache configuration file
 *                        
 * # OSCache standard configuration per
 * #     http://www.opensymphony.com/oscache/wiki/Configuration.html
 * # ---------------------------------------------------------------
 *                        
 * #cache.memory=true
 * #cache.blocking=false
 * cache.capacity=5000
 * cache.algorithm=com.opensymphony.oscache.base.algorithm.LRUCache
 *                        
 * # Cayenne specific properties
 * # ---------------------------------------------------------------
 *                        
 * # Default refresh period in seconds:
 * cayenne.default.refresh = 60
 *                        
 * # Default expiry specified as cron expressions per
 * #    http://www.opensymphony.com/oscache/wiki/Cron%20Expressions.html
 * # expire entries every hour on the 10's minute
 * cayenne.default.cron = 10 * * * *
 *                        
 * # Same parameters can be overriden per query
 * cayenne.group.xyz.refresh = 120
 * cayenne.group.xyz.cron = 10 1 * * *
 * </pre>
 * 
 * Further extension of OSQueryCache is possible by using OSCache listener API.
 * 
 * @since 3.0
 */
public class OSQueryCache implements QueryCache {

    public static final int DEFAULT_REFRESH_PERIOD = CacheEntry.INDEFINITE_EXPIRY;

    static String DEFAULT_REFRESH_KEY = "cayenne.default.refresh";
    static String DEFAULT_CRON_KEY = "cayenne.default.cron";

    static String GROUP_PREFIX = "cayenne.group.";
    static String REFRESH_SUFFIX = ".refresh";
    static String CRON_SUFFIX = ".cron";

    protected GeneralCacheAdministrator osCache;

    RefreshSpecification defaultRefreshSpecification;
    Map<String, RefreshSpecification> refreshSpecifications;
    Properties properties;

    public OSQueryCache() {
        OSCacheAdministrator admin = new OSCacheAdministrator();
        init(admin, admin.getProperties());
    }

    public OSQueryCache(GeneralCacheAdministrator cache, Properties properties) {
        init(cache, properties);
    }

    /**
     * Returns a collection of group names that have been configured explicitly via
     * properties.
     */
    @SuppressWarnings("rawtypes")
    public Collection getGroupNames() {
        return refreshSpecifications != null
                ? Collections.unmodifiableCollection(refreshSpecifications.keySet())
                : Collections.EMPTY_SET;
    }

    public String getCronExpression(String groupName) {

        RefreshSpecification spec = null;

        if (refreshSpecifications != null) {
            spec = refreshSpecifications.get(groupName);
        }

        if (spec == null) {
            spec = defaultRefreshSpecification;
        }

        return spec.cronExpression;
    }

    public int getRrefreshPeriod(String groupName) {

        RefreshSpecification spec = null;

        if (refreshSpecifications != null) {
            spec = refreshSpecifications.get(groupName);
        }

        if (spec == null) {
            spec = defaultRefreshSpecification;
        }

        return spec.refreshPeriod;
    }

    /**
     * Returns the underlying OSCache manager object.
     */
    public GeneralCacheAdministrator getOsCache() {
        return osCache;
    }

    /**
     * Returns configuration properties. Usually this is the contents of
     * "oscache.properties" file.
     */
    public Properties getProperties() {
        return properties;
    }

    void init(GeneralCacheAdministrator cache, Properties properties) {
        this.properties = properties;
        this.osCache = cache;
        this.defaultRefreshSpecification = new RefreshSpecification();

        // load defaults and per-query settings
        if (properties != null) {

            // first extract defaults...
            String defaultRefresh = properties.getProperty(DEFAULT_REFRESH_KEY);
            if (defaultRefresh != null) {
                defaultRefreshSpecification.setRefreshPeriod(defaultRefresh);
            }

            String defaultCron = properties.getProperty(DEFAULT_CRON_KEY);
            if (defaultCron != null) {
                defaultRefreshSpecification.cronExpression = defaultCron;
            }

            // now check for per-query settings
            for (final Map.Entry<Object, Object> entry : properties.entrySet()) {

                if (entry.getKey() == null || entry.getValue() == null) {
                    continue;
                }

                String key = entry.getKey().toString();
                if (key.startsWith(GROUP_PREFIX)) {

                    if (key.endsWith(REFRESH_SUFFIX)) {
                        String name = key.substring(GROUP_PREFIX.length(), key.length()
                                - REFRESH_SUFFIX.length());

                        initRefreshPolicy(name, entry.getValue());
                    }
                    else if (key.endsWith(CRON_SUFFIX)) {
                        String name = key.substring(GROUP_PREFIX.length(), key.length()
                                - CRON_SUFFIX.length());

                        initCronPolicy(name, entry.getValue());
                    }
                }
            }
        }
    }

    /**
     * Called internally for each group that is configured with cron policy in the
     * properties. Exposed mainly for the benefit of subclasses. When overriding, call
     * 'super'.
     */
    protected void initCronPolicy(String groupName, Object value) {
        nonNullSpec(groupName).cronExpression = value != null ? value.toString() : null;
    }

    /**
     * Called internally for each group that is configured with refresh policy in the
     * properties. Exposed mainly for the benefit of subclasses. When overriding, call
     * 'super'.
     */
    protected void initRefreshPolicy(String groupName, Object value) {
        nonNullSpec(groupName).setRefreshPeriod(value);
    }

    private RefreshSpecification nonNullSpec(String name) {
        if (refreshSpecifications == null) {
            refreshSpecifications = new HashMap<String, RefreshSpecification>();
        }

        RefreshSpecification spec = refreshSpecifications.get(name);
        if (spec == null) {
            spec = new RefreshSpecification();
            spec.cronExpression = defaultRefreshSpecification.cronExpression;
            spec.refreshPeriod = defaultRefreshSpecification.refreshPeriod;
            refreshSpecifications.put(name, spec);
        }

        return spec;
    }

    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }

        RefreshSpecification refresh = getRefreshSpecification(metadata);

        try {
            return (List) osCache.getFromCache(
                    key,
                    refresh.refreshPeriod,
                    refresh.cronExpression);
        }
        catch (NeedsRefreshException e) {
            osCache.cancelUpdate(key);
            return null;
        }
    }

    /**
     * Returns a non-null cached value. If it is not present in the cache, it is obtained
     * by calling {@link QueryCacheEntryFactory#createObject()}. Whether the cache
     * provider will block on the entry update or not is controlled by "cache.blocking"
     * configuration property and is "false" by default.
     */
    @SuppressWarnings("rawtypes")
    public List get(QueryMetadata metadata, QueryCacheEntryFactory factory) {
        String key = metadata.getCacheKey();
        if (key == null) {
            return null;
        }

        RefreshSpecification refresh = getRefreshSpecification(metadata);

        try {
            return (List) osCache.getFromCache(
                    key,
                    refresh.refreshPeriod,
                    refresh.cronExpression);
        }
        catch (NeedsRefreshException e) {
            boolean updated = false;
            try {
                Object result = factory.createObject();

                if (!(result instanceof List)) {
                    if (result == null) {
                        throw new CayenneRuntimeException("Null on cache rebuilding: "
                                + metadata.getCacheKey());
                    }
                    else {
                        throw new CayenneRuntimeException(
                                "Invalid query result, expected List, got "
                                        + result.getClass().getName());
                    }
                }

                List list = (List) result;

                put(metadata, list);
                updated = true;
                return list;
            }
            finally {
                if (!updated) {
                    // It is essential that cancelUpdate is called if the
                    // cached content could not be rebuilt
                    osCache.cancelUpdate(key);
                }
            }
        }
    }

    /**
     * Returns non-null RefreshSpecification for the QueryMetadata.
     */
    RefreshSpecification getRefreshSpecification(QueryMetadata metadata) {

        RefreshSpecification refresh = null;

        if (refreshSpecifications != null) {
            String[] groups = metadata.getCacheGroups();
            if (groups != null && groups.length > 0) {
                refresh = refreshSpecifications.get(groups[0]);
            }
        }

        return refresh != null ? refresh : defaultRefreshSpecification;
    }

    @SuppressWarnings("rawtypes")
    public void put(QueryMetadata metadata, List results) {
        String key = metadata.getCacheKey();
        if (key != null) {
            osCache.putInCache(key, results, metadata.getCacheGroups());
        }
    }

    public void remove(String key) {
        if (key != null) {
            osCache.removeEntry(key);
        }
    }

    public void removeGroup(String groupKey) {
        if (groupKey != null) {
            osCache.flushGroup(groupKey);
        }
    }

    public void clear() {
        osCache.flushAll();
    }

    public int size() {
        return osCache.getCache().getSize();
    }

    public int capacity() {
        return osCache.getCache().getCapacity();
    }
    
    /**
     * Shuts down EhCache CacheManager
     */
    @BeforeScopeEnd
    public void shutdown() {
        osCache.destroy();
    }

    final static class RefreshSpecification {

        int refreshPeriod;
        String cronExpression;

        RefreshSpecification() {
            this.refreshPeriod = DEFAULT_REFRESH_PERIOD;
        }

        RefreshSpecification(int refrehsPeriod, String cronExpression) {
            this.refreshPeriod = refrehsPeriod;
            this.cronExpression = cronExpression;
        }

        void setRefreshPeriod(Object value) {
            try {
                refreshPeriod = Integer.parseInt(value.toString());
            }
            catch (NumberFormatException e) {
                // ignore...
            }
        }
    }

    final static class OSCacheAdministrator extends GeneralCacheAdministrator {

        OSCacheAdministrator() {
        }

        OSCacheAdministrator(Properties properties) {
            super(properties);
        }

        Properties getProperties() {
            return super.config.getProperties();
        }
    }
}
