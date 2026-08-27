/*****************************************************************
 *   Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    https://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 ****************************************************************/
package org.apache.cayenne.modeler.ui.project.editor.objentity.callbacks;

import org.apache.cayenne.map.LifecycleEvent;
import org.apache.cayenne.util.Util;

import java.io.Serializable;

public class CallbackType implements Serializable {

    private final LifecycleEvent type;
    private final String name;
    private final int counter;

    public CallbackType(LifecycleEvent type) {
        this.type = type;
        this.name = Util.underscoredToJava(type.name(), true);
        this.counter = 0;
    }

    public LifecycleEvent getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    public String toString() {
        if (counter <= 0) {
            return name;
        } else if (counter == 1) {
            return name + " (1 method)";
        } else {
            return name + " (" + counter + " methods)";
        }
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CallbackType that = (CallbackType) o;

        return type == that.type;
    }

    public int hashCode() {
        return type.hashCode();
    }
}
