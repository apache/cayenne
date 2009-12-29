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

import java.lang.reflect.Constructor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.modeler.pref.DBConnectionInfo;

public class CayenneProjectPreferences {

    // for preferences not dependences from project
    private Map<Class, Object> cayennePreferences;

    // for preferences dependences from project
    private HashMap<Preferences, Object> projectCayennePreferences;

    public CayenneProjectPreferences() {
        cayennePreferences = new HashMap<Class, Object>();
        cayennePreferences.put(DBConnectionInfo.class, new ChildrenMapPreference(
                new DBConnectionInfo()));
        projectCayennePreferences = new HashMap<Preferences, Object>();
        initPreference();
    }

    private void initPreference() {
        Iterator it = cayennePreferences.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            ((ChildrenMapPreference) cayennePreferences.get(pairs.getKey()))
                    .initChildrenPreferences();
        }
    }

    public ChildrenMapPreference getDetailObject(Class className) {
        return (ChildrenMapPreference) cayennePreferences.get(className);
    }

    public CayennePreference getProjectDetailObject(
            Class className,
            Preferences preference) {
        if (projectCayennePreferences.get(preference) == null) {
            try {
                Class cls = className;
                Class partypes[] = new Class[1];
                partypes[0] = Preferences.class;
                Constructor ct = cls.getConstructor(partypes);
                Object arglist[] = new Object[1];
                arglist[0] = preference;
                Object retobj = ct.newInstance(arglist);
                projectCayennePreferences.put(preference, retobj);
            }
            catch (Throwable e) {
                new CayenneRuntimeException("Error initing preference");
            }
        }

        return (CayennePreference) projectCayennePreferences.get(preference);
    }

    // delete property
    public void removeProjectDetailObject(Preferences preference) {
        try {
            preference.removeNode();
            projectCayennePreferences.remove(preference);
        }
        catch (BackingStoreException e) {
            new CayenneRuntimeException("error delete preferences " + e);
        }
    }
}
