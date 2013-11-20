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
package org.apache.cayenne.di.spi;

import java.util.LinkedList;
import java.util.List;

import org.apache.cayenne.di.DIRuntimeException;
import org.apache.cayenne.di.Key;

/**
 * A helper object that tracks the injection stack to prevent circular dependencies.
 * 
 * @since 3.1
 */
class InjectionStack {

    private ThreadLocal<LinkedList<Key<?>>> stack;

    InjectionStack() {
        this.stack = new ThreadLocal<LinkedList<Key<?>>>();
    }

    void reset() {
        List<Key<?>> localStack = stack.get();
        if (localStack != null) {
            localStack.clear();
        }
    }

    void push(Key<?> bindingKey) throws DIRuntimeException {
        LinkedList<Key<?>> localStack = stack.get();
        if (localStack == null) {
            localStack = new LinkedList<Key<?>>();
            stack.set(localStack);
        }

        if (localStack.contains(bindingKey)) {
            throw new DIRuntimeException(
                    "Circular dependency detected when binding a key \"%s\". Nested keys: %s"
                            + ". To resolve it, you should inject a Provider instead of an object.",
                    bindingKey,
                    localStack);
        }

        localStack.add(bindingKey);
    }

    void pop() {
        LinkedList<Key<?>> localStack = stack.get();
        if (localStack != null) {
            localStack.removeLast();
        }
        else {
            throw new IndexOutOfBoundsException("0");
        }
    }

    @Override
    public String toString() {
        List<Key<?>> localStack = stack.get();
        if (localStack != null) {
            return String.valueOf(localStack);
        }
        else {
            return "[]";
        }
    }
}
