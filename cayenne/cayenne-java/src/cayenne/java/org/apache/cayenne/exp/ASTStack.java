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

package org.apache.cayenne.exp;

import java.util.LinkedList;

/**
 * AST evaluation stack.
 * 
 * @author Andrei Adamchik
 * @since 1.0.6
 * @deprecated since 1.2
 */
final class ASTStack extends LinkedList {
    static boolean booleanFromObject(Object object) {
        // TODO: pull conversion to boolean to utils, or use 3rd party converter
        if (object instanceof Boolean) {
            return ((Boolean) object).booleanValue();
        }

        if (object instanceof Number) {
            return ((Number) object).intValue() != 0;
        }

        return object != null;
    }

    /** 
      * Pops a value from the stack.
      */
    Object pop() {
        return remove(size() - 1);
    }

    /** 
     * Pops a value from the stack, converting it to boolean.
     */
    boolean popBoolean() {
        Object obj = pop();
        return booleanFromObject(obj);
    }

    /** 
     * Pops a value from the stack, casting it to Comparable.
     */
    Comparable popComparable() {
        return (Comparable) pop();
    }

    /** 
     * Pops a value from the stack, converting it to int.
     */
    int popInt() {
        return ((Integer) pop()).intValue();
    }

    /** 
     * Returns a value from the stack without removing it.
     */
    public Object peekObject() {
        return get(size() - 1);
    }

    /** 
     * Returns a value from the stack without removing it, converting it to boolean.
     */
    boolean peekBoolean() {
        Object obj = peekObject();
        return booleanFromObject(obj);
    }

    /** 
     * Returns a value from the stack without removing it, converting it to int.
     */
    int peekInt() {
        return ((Integer) peekObject()).intValue();
    }

    /**
     * Pushes a value to the stack.
     */
    void push(Object obj) {
        add(obj);
    }

    /**
     * Pushes a boolean value to the stack.
     */
    void push(boolean b) {
        add(b ? Boolean.TRUE : Boolean.FALSE);
    }
}
