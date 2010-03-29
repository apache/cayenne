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

import org.apache.cayenne.access.DataContext;

/**
 * An editor for modifying CayennePreferenceService.
 * 
 */
public abstract class CayennePreferenceEditor implements PreferenceEditor {

    protected CayennePreferenceService service;
    protected DataContext editingContext;
    protected boolean restartRequired;
    protected int saveInterval;

    public CayennePreferenceEditor(CayennePreferenceService service) {
        this.service = service;
        this.editingContext = service
                .getDataContext()
                .getParentDataDomain()
                .createDataContext();
        this.saveInterval = service.getSaveInterval();
    }

    protected boolean isRestartRequired() {
        return restartRequired;
    }

    protected void setRestartRequired(boolean restartOnSave) {
        this.restartRequired = restartOnSave;
    }

    protected DataContext getEditingContext() {
        return editingContext;
    }

    public Domain editableInstance(Domain object) {
        if (object.getObjectContext() == getEditingContext()) {
            return object;
        }

        return (Domain) getEditingContext().localObject(object.getObjectId(), null);
    }

    public PreferenceDetail createDetail(Domain domain, String key) {
        domain = editableInstance(domain);
        DomainPreference preference = getEditingContext().newObject(DomainPreference.class);
        preference.setDomain(domain);
        preference.setKey(key);

        return preference.getPreference();
    }

    public PreferenceDetail createDetail(Domain domain, String key, Class javaClass) {
        domain = editableInstance(domain);
        DomainPreference preferenceLink = getEditingContext().newObject(DomainPreference.class);
        preferenceLink.setDomain(domain);
        preferenceLink.setKey(key);

        PreferenceDetail detail = (PreferenceDetail) getEditingContext().newObject(
                javaClass);

        detail.setDomainPreference(preferenceLink);
        return detail;
    }

    public PreferenceDetail deleteDetail(Domain domain, String key) {
        domain = editableInstance(domain);
        PreferenceDetail detail = domain.getDetail(key, false);

        if (detail != null) {
            DomainPreference preference = detail.getDomainPreference();
            preference.setDomain(null);
            getEditingContext().deleteObject(preference);
            getEditingContext().deleteObject(detail);
        }

        return detail;
    }

    public int getSaveInterval() {
        return saveInterval;
    }

    public void setSaveInterval(int ms) {
        if (saveInterval != ms) {
            saveInterval = ms;
            restartRequired = true;
        }
    }

    public PreferenceService getService() {
        return service;
    }

    public void save() {
        service.setSaveInterval(saveInterval);
        editingContext.commitChanges();

        if (restartRequired) {
            restart();
        }
    }

    public void revert() {
        editingContext.rollbackChanges();
        restartRequired = false;
    }

    protected abstract void restart();
}
