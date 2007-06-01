/* ====================================================================
 * 
 * The ObjectStyle Group Software License, version 1.1
 * ObjectStyle Group - http://objectstyle.org/
 * 
 * Copyright (c) 2002-2004, Andrei (Andrus) Adamchik and individual authors
 * of the software. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 * 
 * 3. The end-user documentation included with the redistribution, if any,
 *    must include the following acknowlegement:
 *    "This product includes software developed by independent contributors
 *    and hosted on ObjectStyle Group web site (http://objectstyle.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 * 
 * 4. The names "ObjectStyle Group" and "Cayenne" must not be used to endorse
 *    or promote products derived from this software without prior written
 *    permission. For written permission, email
 *    "andrus at objectstyle dot org".
 * 
 * 5. Products derived from this software may not be called "ObjectStyle"
 *    or "Cayenne", nor may "ObjectStyle" or "Cayenne" appear in their
 *    names without prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE OBJECTSTYLE GROUP OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 * 
 * This software consists of voluntary contributions made by many
 * individuals and hosted on ObjectStyle Group web site.  For more
 * information on the ObjectStyle Group, please see
 * <http://objectstyle.org/>.
 */
package org.objectstyle.cayenne.exp;

import java.util.LinkedList;

/**
 * AST evaluation stack.
 * 
 * @author Andrei Adamchik
 * @since 1.0.6
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
    Object peek() {
        return get(size() - 1);
    }

    /** 
     * Returns a value from the stack without removing it, converting it to boolean.
     */
    boolean peekBoolean() {
        Object obj = peek();
        return booleanFromObject(obj);
    }

    /** 
     * Returns a value from the stack without removing it, converting it to int.
     */
    int peekInt() {
        return ((Integer) peek()).intValue();
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