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

package org.apache.cayenne.pref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.exp.Expression;
import org.apache.cayenne.exp.ExpressionFactory;
import org.apache.cayenne.map.DbAttribute;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.query.NamedQuery;
import org.apache.cayenne.query.SelectQuery;
import org.apache.cayenne.util.Util;

/**
 * Preferences "domain" is logical area for preferences storage. Domains are organized in
 * a tree hierarchy allowing cascading preferences lookup.
 * 
 */
public class Domain extends _Domain {

    /**
     * Renames this domain. If there is a sibling domain with same name, such sibling is
     * renamed using generated unique name. This operation essentially substitutes one
     * domain subtree with another.
     */
    public void rename(String newName) {
        if (Util.nullSafeEquals(getName(), newName)) {
            return;
        }

        Domain parent = getParentDomain();
        if (parent == null) {
            setName(newName);
            return;
        }

        Domain other = parent.getSubdomain(newName, false);
        if (other != null && other != this) {
            String otherName = null;
            for (int i = 1; i < 1000; i++) {
                if (parent.getSubdomain(newName + i, false) == null) {
                    otherName = newName + i;
                    break;
                }
            }

            if (otherName == null) {
                throw new PreferenceException("Can't rename an existing domain '"
                        + newName
                        + "'.");
            }

            other.setName(otherName);
        }
        setName(newName);
    }

    /**
     * Returns a direct child of this domain that should handle preferences for all
     * instances of a given Java class. Creates such subdomain if it doesn't exist.
     */
    public Domain getSubdomain(Class javaClass) {
        return getSubdomain(javaClass.getName());
    }

    /**
     * Returns named subdomain. Creates such subdomain if it doesn't exist. Named
     * subdomains are useful when preferences have to be assigned based on a more
     * fine-grained criteria than a Java class. E.g. a class Project can have preferences
     * subdomain for each project location.
     */
    public Domain getSubdomain(String subdomainName) {
        return getSubdomain(subdomainName, true);
    }

    public Domain getSubdomain(String subdomainName, boolean create) {
        List subdomains = getSubdomains();

        if (subdomains.size() > 0) {
            List matching = ExpressionFactory.matchExp(
                    Domain.NAME_PROPERTY,
                    subdomainName).filterObjects(subdomains);
            if (matching.size() > 0) {
                return (Domain) matching.get(0);
            }
        }

        if (!create) {
            return null;
        }

        Domain childSubdomain = getObjectContext().newObject(Domain.class);
        addToSubdomains(childSubdomain);
        childSubdomain.setName(subdomainName);

        if (getLevel() == null) {
            throw new CayenneRuntimeException("Null level, can't create child");
        }

        int level = getLevel().intValue() + 1;
        childSubdomain.setLevel(new Integer(level));
        getObjectContext().commitChanges();

        return childSubdomain;
    }

    /**
     * Returns all generic preferences for the domain.
     */
    public List getDetails() {
        Collection domainPrefs = getPreferences();

        if (domainPrefs.isEmpty()) {
            // return mutable list
            return new ArrayList(1);
        }

        List details = new ArrayList(domainPrefs.size());
        Iterator it = domainPrefs.iterator();
        while (it.hasNext()) {
            DomainPreference preference = (DomainPreference) it.next();
            details.add(preference.getPreference());
        }
        return details;
    }

    /**
     * Returns a preference object used to read/write properties.
     */
    public PreferenceDetail getDetail(String key, boolean create) {
        return getDetail(key, null, create);
    }

    /**
     * Returns all stored PreferenceDetails for a given class in this Domain.
     */
    public Collection getDetails(Class javaClass) {
        // extract preference ids, and then fetch matching prefrences...
        Collection preferences = getPreferences();
        if (preferences.isEmpty()) {
            return Collections.EMPTY_LIST;
        }

        Collection ids = new ArrayList(preferences.size());
        Iterator it = preferences.iterator();
        while (it.hasNext()) {
            DomainPreference pref = (DomainPreference) it.next();
            ids.add(DataObjectUtils.pkForObject(pref));
        }

        ObjectContext context = getObjectContext();
        DbEntity entity = context.getEntityResolver().lookupDbEntity(javaClass);
        DbAttribute pk = entity.getPrimaryKeys().iterator().next();

        Expression qualifier = Expression.fromString("db:" + pk.getName() + " in $ids");
        Map params = Collections.singletonMap("ids", ids);
        SelectQuery query = new SelectQuery(javaClass, qualifier
                .expWithParameters(params));
        return context.performQuery(query);
    }

    /**
     * Returns preference details keyed using their master key.
     */
    public Map getDetailsMap(Class javaClass) {
        Collection details = getDetails(javaClass);
        Map map = new HashMap();

        if (details.isEmpty()) {
            return map;
        }

        Iterator it = details.iterator();
        while (it.hasNext()) {
            PreferenceDetail detail = (PreferenceDetail) it.next();
            map.put(detail.getKey(), detail);
        }

        return map;
    }

    /**
     * Locates a PreferenceDetail in a current Domain for a given key and Java class. If
     * no such preference found, and "create" is true, a new preference is created. If an
     * existing preference class does not match supplied class, PreferenceException is
     * thrown.
     */
    public PreferenceDetail getDetail(String key, Class javaClass, boolean create) {
        DomainPreference preferenceLink = getDomainPreference(key);

        if (preferenceLink == null) {

            if (!create) {
                return null;
            }

            preferenceLink = getObjectContext().newObject(DomainPreference.class);
            preferenceLink.setDomain(this);
            preferenceLink.setKey(key);
            getObjectContext().commitChanges();
        }

        return (javaClass == null) ? preferenceLink.getPreference() : preferenceLink
                .getPreference(javaClass, create);
    }

    /**
     * Looks up a preference for key in the domain hierarchy.
     */
    DomainPreference getDomainPreference(String key) {
        // query sorts preferences by subdomain level, so the first object is the lowest
        // one
        Map params = new HashMap();
        params.put("key", key);
        params.put("domain", this);
        List results = getObjectContext().performQuery(
                new NamedQuery("DomainPreferenceForKey", params));
        return (results.size() < 1) ? null : (DomainPreference) results.get(0);
    }
}
