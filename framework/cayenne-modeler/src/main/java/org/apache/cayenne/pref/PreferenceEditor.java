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

/**
 * Defines an API of a preferences editor used for editing preferences without affecting
 * the rest of the application until the editing is finished.
 * 
 */
public interface PreferenceEditor {

    /**
     * Returns an underlying PreferenceService which is a parent of this editor.
     */
    public PreferenceService getService();

    /**
     * Creates a generic PreferenceDetail.
     */
    public PreferenceDetail createDetail(Domain domain, String key);

    /**
     * Creates PreferenceDetail of specified class.
     */
    public PreferenceDetail createDetail(Domain domain, String key, Class javaClass);

    public PreferenceDetail deleteDetail(Domain domain, String key);

    public Domain editableInstance(Domain domain);

    public void save();

    public void revert();
}
