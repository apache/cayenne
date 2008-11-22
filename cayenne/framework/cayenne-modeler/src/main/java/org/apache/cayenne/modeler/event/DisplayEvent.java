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

package org.apache.cayenne.modeler.event;

import java.util.EventObject;

import org.apache.cayenne.project.ProjectPath;

/**
 */
public class DisplayEvent extends EventObject {
    protected boolean refired;
    protected boolean changed;
    protected ProjectPath path;

    /**
     * Constructor for DisplayEvent.
     * @param source
     */
    public DisplayEvent(Object source) {
        super(source);
        refired = false;
        changed = true;
    }

    /**
     * Constructor for DisplayEvent.
     * @param source
     */
    public DisplayEvent(Object source, ProjectPath path) {
        super(source);
        refired = false;
        changed = true;
        this.path = path;
    }

    public ProjectPath getPath() {
        return path;
    }

    public void setPath(ProjectPath path) {
        this.path = path;
    }

    /**
    * Returns the last object in the path.
    */
    public Object getObject() {
        return (path != null) ? path.getObject() : null;
    }

    /**
     * Returns true if the path's last object has changed.
     */
    public boolean isChanged() {
        return changed;
    }

    public void setChanged(boolean changed) {
        this.changed = changed;
    }

    /**
     * Returns true if the event has been refired
     */
    public boolean isRefired() {
        return refired;
    }

    public void setRefired(boolean refired) {
        this.refired = refired;
    }

    public boolean pointsTo(Class nodeClass) {
        if (nodeClass == null) {
            return false;
        }

        Object last = getObject();
        return (last != null) ? last.getClass() == nodeClass : false;
    }
}
