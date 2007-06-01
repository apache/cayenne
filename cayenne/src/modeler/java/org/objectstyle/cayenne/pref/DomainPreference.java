/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.objectstyle.cayenne.DataObjectUtils;
import org.objectstyle.cayenne.util.Util;

/**
 * @author Andrei Adamchik
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
                getDataContext(),
                javaClass,
                pk);

        if (preference != null) {
            preference.setDomainPreference(this);
        }

        if (preference != null || !create) {
            return preference;
        }

        preference = (PreferenceDetail) getDataContext().createAndRegisterNewObject(
                javaClass);

        preference.setDomainPreference(this);
        getDataContext().commitChanges();
        return preference;
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

