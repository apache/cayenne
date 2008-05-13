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

package org.apache.cayenne.jpa.map;

import org.apache.cayenne.util.XMLEncoder;
import org.apache.cayenne.util.XMLSerializable;

public class JpaEntityListener implements XMLSerializable {

    protected String className;
    protected JpaLifecycleCallback prePersist;
    protected JpaLifecycleCallback postPersist;
    protected JpaLifecycleCallback preRemove;
    protected JpaLifecycleCallback postRemove;
    protected JpaLifecycleCallback preUpdate;
    protected JpaLifecycleCallback postUpdate;
    protected JpaLifecycleCallback postLoad;
    
    public void encodeAsXML(XMLEncoder encoder) {
    }

    public JpaLifecycleCallback getPostLoad() {
        return postLoad;
    }

    public void setPostLoad(JpaLifecycleCallback postLoad) {
        this.postLoad = postLoad;
    }

    public JpaLifecycleCallback getPostPersist() {
        return postPersist;
    }

    public void setPostPersist(JpaLifecycleCallback postPersist) {
        this.postPersist = postPersist;
    }

    public JpaLifecycleCallback getPostRemove() {
        return postRemove;
    }

    public void setPostRemove(JpaLifecycleCallback postRemove) {
        this.postRemove = postRemove;
    }

    public JpaLifecycleCallback getPostUpdate() {
        return postUpdate;
    }

    public void setPostUpdate(JpaLifecycleCallback postUpdate) {
        this.postUpdate = postUpdate;
    }

    public JpaLifecycleCallback getPrePersist() {
        return prePersist;
    }

    public void setPrePersist(JpaLifecycleCallback prePersist) {
        this.prePersist = prePersist;
    }

    public JpaLifecycleCallback getPreRemove() {
        return preRemove;
    }

    public void setPreRemove(JpaLifecycleCallback preRemove) {
        this.preRemove = preRemove;
    }

    public JpaLifecycleCallback getPreUpdate() {
        return preUpdate;
    }

    public void setPreUpdate(JpaLifecycleCallback preUpdate) {
        this.preUpdate = preUpdate;
    }

    public String getClassName() {
        return className;
    }

    public void setClassName(String className) {
        this.className = className;
    }

    @Override
    public String toString() {
        return "JpaEntityListener:" + className;
    }
}
