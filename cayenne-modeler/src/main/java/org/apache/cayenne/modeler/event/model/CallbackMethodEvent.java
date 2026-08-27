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
package org.apache.cayenne.modeler.event.model;

public class CallbackMethodEvent extends ModelEvent {

    private final String callbackMethod;

    public static CallbackMethodEvent ofAdd(Object source, String callbackMethod) {
        return new CallbackMethodEvent(source, callbackMethod, Type.ADD, null);
    }

    public static CallbackMethodEvent ofChange(Object source, String callbackMethod, String oldCallbackMethod) {
        return new CallbackMethodEvent(source, callbackMethod, Type.CHANGE, oldCallbackMethod);
    }

    public static CallbackMethodEvent ofRemove(Object source, String callbackMethod) {
        return new CallbackMethodEvent(source, callbackMethod, Type.REMOVE, null);
    }

    private CallbackMethodEvent(Object source, String callbackMethod, Type type, String oldName) {
        super(source, type, oldName);
        this.callbackMethod = callbackMethod;
    }

    @Override
    public String getNewName() {
        return callbackMethod;
    }
}
