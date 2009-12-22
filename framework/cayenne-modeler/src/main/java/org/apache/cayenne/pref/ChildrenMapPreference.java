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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.prefs.BackingStoreException;
import java.util.prefs.Preferences;

import org.apache.cayenne.CayenneRuntimeException;


public class ChildrenMapPreference extends CayennePreferenceDecorator {

    private Map<String, Object> childrens;
    private List<String> removeObject;

    public ChildrenMapPreference(CayennePreference decoratedPreference) {
        super(decoratedPreference);
        this.childrens = new HashMap<String, Object>();
        this.removeObject = new ArrayList<String>();
    }

    public Preferences getCayennePreference() {
        return decoratedPreference.getCayennePreference();
    }

    public Preferences getRootPreference() {
        return decoratedPreference.getRootPreference();
    }

    public void initChildrenPreferences() {
        Map<String, Object> children = new HashMap<String, Object>();
        try {
            String[] names = getCurrentPreference().childrenNames();
            for (int i = 0; i < names.length; i++) {

                try {
                    Class cls = decoratedPreference.getClass();
                    Class partypes[] = new Class[2];
                    partypes[0] = String.class;
                    partypes[1] = boolean.class;
                    Constructor ct = cls.getConstructor(partypes);
                    Object arglist[] = new Object[2];
                    arglist[0] = names[i];
                    arglist[1] = true;
                    Object retobj = ct.newInstance(arglist);
                    children.put(names[i], retobj);
                }
                catch (Throwable e) {
                    new CayenneRuntimeException("Error initing preference");
                }

            }

            this.childrens.putAll(children);
        }
        catch (BackingStoreException e) {
            e.printStackTrace();
        }
    }

    public Map getChildrenPreferences() {
        return childrens;
    }

    public Preferences getCurrentPreference() {
        return decoratedPreference.getCurrentPreference();
    }

    public CayennePreference getObject(String key) {
        return (CayennePreference) childrens.get(key);
    }

    public void remove(String key) {
        removeObject.add(key);
        childrens.remove(key);
    }

    public CayennePreference create(String nodeName) {
        try {
            Class cls = decoratedPreference.getClass();
            Class partypes[] = new Class[2];
            partypes[0] = String.class;
            partypes[1] = boolean.class;
            Constructor ct = cls.getConstructor(partypes);
            Object arglist[] = new Object[2];
            arglist[0] = nodeName;
            arglist[1] = false;
            Object retobj = ct.newInstance(arglist);
            childrens.put(nodeName, retobj);
        }
        catch (Throwable e) {
            new CayenneRuntimeException("Error creating preference");
        }
        return (CayennePreference) childrens.get(nodeName);
    }

    public void create(String nodeName, Object obj) {
        childrens.put(nodeName, obj);
    }

    public void save() {
        if (removeObject.size() > 0) {
            for (int i = 0; i < removeObject.size(); i++) {
                try {
                    decoratedPreference
                            .getCurrentPreference()
                            .node(removeObject.get(i))
                            .removeNode();
                }
                catch (BackingStoreException e) {
                    new CayenneRuntimeException("Error saving preference");
                }
            }
        }
        
        // наверное стоит как-то помечать чтобы не все пересохранять
        // как?
        // !!!!!!!!!!!!!!!!!
        
        
        Iterator it = childrens.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry)it.next();
            
            decoratedPreference.getCurrentPreference().node((String) pairs.getKey());
            ((CayennePreference) pairs.getValue())
                    .saveObjectPreference();
            
        }
    }

    public void cancel() {
        this.childrens.clear();
        initChildrenPreferences();
    }
}
