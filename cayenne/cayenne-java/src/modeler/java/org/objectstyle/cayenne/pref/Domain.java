/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2005, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.pref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.objectstyle.cayenne.CayenneRuntimeException;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.exp.Expression;
import org.objectstyle.cayenne.exp.ExpressionFactory;
import org.objectstyle.cayenne.map.DbAttribute;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.query.SelectQuery;
import org.objectstyle.cayenne.util.Util;

/**
 * Preferences "domain" is logical area for preferences storage. Domains are organized in
 * a tree hierarchy allowing cascading preferences lookup.
 * 
 * @author Andrei Adamchik
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

        Domain childSubdomain = (Domain) getDataContext().createAndRegisterNewObject(
                Domain.class);
        addToSubdomains(childSubdomain);
        childSubdomain.setName(subdomainName);

        if (getLevel() == null) {
            throw new CayenneRuntimeException("Null level, can't create child");
        }

        int level = getLevel().intValue() + 1;
        childSubdomain.setLevel(new Integer(level));
        getDataContext().commitChanges();

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

        DataContext context = getDataContext();
        DbEntity entity = context.getEntityResolver().lookupDbEntity(javaClass);
        DbAttribute pk = (DbAttribute) entity.getPrimaryKey().get(0);

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

            preferenceLink = (DomainPreference) getDataContext()
                    .createAndRegisterNewObject(DomainPreference.class);
            preferenceLink.setDomain(this);
            preferenceLink.setKey(key);
            getDataContext().commitChanges();
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
        List results = getDataContext().performQuery(
                "DomainPreferenceForKey",
                params,
                false);
        return (results.size() < 1) ? null : (DomainPreference) results.get(0);
    }
}

