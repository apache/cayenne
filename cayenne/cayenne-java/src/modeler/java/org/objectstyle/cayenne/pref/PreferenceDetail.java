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

import java.util.Properties;

import org.objectstyle.cayenne.CayenneDataObject;
import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.ObjectId;
import org.objectstyle.cayenne.access.DataContext;
import org.objectstyle.cayenne.access.DataNode;
import org.objectstyle.cayenne.map.DbEntity;
import org.objectstyle.cayenne.util.Util;

/**
 * A superclass of concrete preference classes.
 * <p>
 * Complete preference descriptor is composed out of two classes - DomainPreference that
 * defines how the preference is located with in domain, and a GenericPreference.
 * GenericPreference API is designed for the application use, while internal
 * DomainPreference is managed behind the scenes. Note that there is no real Cayenne
 * relationship from concrete preference entity to the preference framework entities, so
 * this class handles all needed wiring...
 * 
 * @author Andrei Adamchik
 */
public class PreferenceDetail extends CayenneDataObject {

    protected DomainPreference domainPreference;

    /**
     * Changes the key of this preference. If there is a sibling prefrence with same key,
     * such sibling is renamed using generated unique name. This operation essentially
     * substitutes one prefrence entry with another.
     */
    public void rename(String newKey) {
        if (Util.nullSafeEquals(getKey(), newKey)) {
            return;
        }

        DomainPreference domainPrefrence = getDomainPreference();
        Domain parent = domainPrefrence.getDomain();

        if (parent == null) {
            domainPrefrence.setKey(newKey);
            return;
        }

        DomainPreference other = parent.getDomainPreference(newKey);
        if (other != null && other != domainPrefrence) {
            String otherName = null;
            for (int i = 1; i < 1000; i++) {
                if (parent.getDomainPreference(newKey + i) == null) {
                    otherName = newKey + i;
                    break;
                }
            }

            if (otherName == null) {
                throw new PreferenceException("Can't rename an existing preference '"
                        + newKey
                        + "'.");
            }

            other.setKey(otherName);
        }

        domainPrefrence.setKey(newKey);
    }

    public int getIntProperty(String key, int defaultValue) {
        String value = getProperty(key);

        if (value == null) {
            return defaultValue;
        }

        try {
            return Integer.parseInt(value);
        }
        catch (NumberFormatException ex) {
            throw new PreferenceException("Error converting to int: " + value);
        }
    }

    public String getKey() {
        if (getDomainPreference() == null) {
            throw new PreferenceException(
                    "Preference not initialized, can't work with properties.");
        }

        return domainPreference.getKey();
    }

    public void setIntProperty(String key, int value) {
        setProperty(key, String.valueOf(value));
    }

    /**
     * Returns a named property for a given key.
     */
    public String getProperty(String key) {
        return getProperties().getProperty(key);
    }

    public void setProperty(String key, String value) {
        getProperties().setProperty(key, value);
    }

    public boolean getBooleanProperty(String key) {
        return "true".equalsIgnoreCase(getProperty(key));
    }

    public void setBooleanProperty(String key, boolean value) {
        setProperty(key, "" + value);
    }

    public DomainPreference getDomainPreference() {
        if (domainPreference == null) {
            // try to fetch..

            DataContext context = getDataContext();

            if (context != null && getObjectId() != null) {
                int pk = DataObjectUtils.intPKForObject(this);

                domainPreference = (DomainPreference) DataObjectUtils.objectForPK(
                        context,
                        DomainPreference.class,
                        pk);

            }
        }

        return domainPreference;
    }

    /**
     * Initializes internal DomainPreference object.
     */
    public void setDomainPreference(DomainPreference domainPreference) {
        if (this.domainPreference != domainPreference) {
            this.domainPreference = domainPreference;

            ObjectId oid = getObjectId();
            if (oid != null && oid.isTemporary()) {
                oid.getReplacementIdMap().put("id", new Integer(buildPermanentId()));
            }
        }
    }

    /**
     * Returns initialized non-null properties map.
     */
    protected Properties getProperties() {
        if (getDomainPreference() == null) {
            throw new PreferenceException(
                    "Preference not initialized, can't work with properties.");
        }

        return domainPreference.getProperties();
    }

    /**
     * Creates permanent ID based on DomainPreference id.
     */
    protected int buildPermanentId() {
        ObjectId otherId = getDomainPreference().getObjectId();
        if (otherId == null) {
            throw new PreferenceException(
                    "Can't persist preference. DomainPreference has no ObjectId");
        }

        // force creation of otherId
        if (otherId.isTemporary() && !otherId.isReplacementIdAttached()) {
            DbEntity entity = getDataContext().getEntityResolver().lookupDbEntity(
                    domainPreference);

            DataNode node = getDataContext().getParentDataDomain().lookupDataNode(
                    entity.getDataMap());

            try {
                Object pk = node.getAdapter().getPkGenerator().generatePkForDbEntity(
                        node,
                        entity);
                otherId.getReplacementIdMap().put(DomainPreference.ID_PK_COLUMN, pk);
            }
            catch (Throwable th) {
                throw new PreferenceException("Error creating primary key", Util
                        .unwindException(th));
            }
        }

        return DataObjectUtils.intPKForObject(domainPreference);
    }
}