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

import java.util.Properties;

import org.apache.cayenne.CayenneDataObject;
import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.ObjectContext;
import org.apache.cayenne.ObjectId;
import org.apache.cayenne.access.DataContext;
import org.apache.cayenne.access.DataNode;
import org.apache.cayenne.map.DbEntity;
import org.apache.cayenne.util.Util;

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

            ObjectContext context = getObjectContext();

            if (context != null && getObjectId() != null) {
                int pk = DataObjectUtils.intPKForObject(this);

                domainPreference = DataObjectUtils.objectForPK(
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
            DbEntity entity = getObjectContext().getEntityResolver().lookupDbEntity(
                    domainPreference);

            DataNode node = ((DataContext) getObjectContext())
                    .getParentDataDomain()
                    .lookupDataNode(entity.getDataMap());

            try {
                Object pk = node.getAdapter().getPkGenerator().generatePk(
                        node,
                        entity.getPrimaryKeys().iterator().next());
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
