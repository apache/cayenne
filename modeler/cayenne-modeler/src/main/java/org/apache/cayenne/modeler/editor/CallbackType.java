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
package org.apache.cayenne.modeler.editor;

import org.apache.cayenne.map.LifecycleEvent;

/**
 * Entity for callback type. Contains type and type name
 * 
 * @version 1.0 Oct 26, 2007
 */

public class CallbackType {

    private LifecycleEvent type;

    /**
     * callback type name
     */
    private String name;

    /**
     * methods counter
     */
    private int counter;

    /**
     * constructor
     * 
     * @param type type id
     * @param name name
     */
    public CallbackType(LifecycleEvent type, String name) {
        this.type = type;
        this.name = name;
        this.counter = 0;
    }

    /**
     * @return callback type id
     */
    public LifecycleEvent getType() {
        return type;
    }

    /**
     * @return callback name
     */
    public String getName() {
        return name;
    }

    /**
     * @return number of callback methods of this type.
     */
    public int getCounter() {
        return counter;
    }

    /**
     * Method to specify counter value
     * 
     * @param counter new coutner value
     */
    public void setCounter(int counter) {
        this.counter = counter;
    }

    /**
     * @return a human readable label representing for this callback.
     */
    public String toString() {
        if (counter <= 0) {
            return name;
        }
        else if (counter == 1) {
            return name + " (1 method)";
        }
        else {
            return name + " (" + counter + " methods)";
        }
    }

    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;

        CallbackType that = (CallbackType) o;

        if (type != that.type)
            return false;

        return true;
    }

    public int hashCode() {
        return type.hashCode();
    }
}
