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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.apache.cayenne.DataObjectUtils;
import org.apache.cayenne.PersistenceState;
import org.apache.cayenne.util.Util;

/**
 */
public class DomainPreference extends _DomainPreference {

    protected DomainProperties properties;

    protected Properties getProperties() {

        if (properties == null) {
            properties = new DomainProperties();

            String values = getKeyValuePairs();
            if (values != null && values.length() > 0) {
                try {
                    properties.load(new ByteArrayInputStream(values.getBytes()));
                }
                catch (IOException ex) {
                    throw new PreferenceException("Error loading properties.", ex);
                }
            }
        }

        return properties;
    }

    protected void encodeProperties() {
        if (this.properties == null) {
            setKeyValuePairs(null);
        }
        else {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try {
                properties.store(out, null);
            }
            catch (IOException ex) {
                throw new PreferenceException("Error storing properties.", ex);
            }

            setKeyValuePairs(out.toString());
        }
    }

    /**
     * Returns a generic preference for associated with a given DomainPreference.
     */
    protected PreferenceDetail getPreference() {
        PreferenceDetail preference = new PreferenceDetail();
        preference.setDomainPreference(this);
        return preference;
    }

    /**
     * Locates and returns a detail preference of a given class.
     */
    protected PreferenceDetail getPreference(Class javaClass, boolean create) {

        // detail object PK must match...

        int pk = DataObjectUtils.intPKForObject(this);
        PreferenceDetail preference = (PreferenceDetail) DataObjectUtils.objectForPK(
                getObjectContext(),
                javaClass,
                pk);

        if (preference != null) {
            preference.setDomainPreference(this);
        }

        if (preference != null || !create) {
            return preference;
        }

        preference = (PreferenceDetail) getObjectContext().newObject(javaClass);

        preference.setDomainPreference(this);
        getObjectContext().commitChanges();
        return preference;
    }

    /**
     * Overrides super implementation to handle non-persistent properties on object state
     * changes.
     */
    public void setPersistenceState(int state) {

        // if invalidated
        if (state == PersistenceState.HOLLOW) {
            properties = null;
        }

        super.setPersistenceState(state);
    }

    class DomainProperties extends Properties {

        public Object setProperty(String key, String value) {
            Object old = super.setProperty(key, value);
            if (!Util.nullSafeEquals(old, value)) {
                modified();
            }

            return old;
        }

        void modified() {
            // more efficient implementation should only call encode on commit using
            // DataContext events... still there is a bug that prevents DataObject from
            // changing its state during "willCommit", so for now do this...
            encodeProperties();
        }
    }

}
