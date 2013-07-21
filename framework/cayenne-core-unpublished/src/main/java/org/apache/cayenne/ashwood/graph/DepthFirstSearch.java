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
/* ====================================================================
 *
 * Copyright(c) 2003, Andriy Shapochka
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 *    copyright notice, this list of conditions and the following
 *    disclaimer in the documentation and/or other materials
 *    provided with the distribution.
 *
 * 3. Neither the name of the ASHWOOD nor the
 *    names of its contributors may be used to endorse or
 *    promote products derived from this software without
 *    specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 *
 * ====================================================================
 *
 * This software consists of voluntary contributions made by
 * individuals on behalf of the ASHWOOD Project and was originally
 * created by Andriy Shapochka.
 *
 */
package org.apache.cayenne.ashwood.graph;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.commons.collections.ArrayStack;

/**
 * @since 3.1
 */
public class DepthFirstSearch<E> implements Iterator<E> {

    protected DigraphIteration<E, ?> factory;
    protected E firstVertex;

    protected ArrayStack stack = new ArrayStack();
    protected Set<E> seen = new HashSet<E>();

    public DepthFirstSearch(DigraphIteration<E, ?> factory, E firstVertex) {
        this.factory = factory;
        this.firstVertex = firstVertex;
        stack.push(factory.outgoingIterator(firstVertex));
        seen.add(firstVertex);
    }

    public void reset(E newFirstVertex) {
        stack.clear();
        seen.clear();
        firstVertex = newFirstVertex;
        stack.push(factory.outgoingIterator(firstVertex));
        seen.add(firstVertex);
    }

    public boolean hasNext() {
        return !stack.isEmpty();
    }

    public E next() {
        ArcIterator<E, ?> i = (ArcIterator<E, ?>) stack.pop();
        Object origin = i.getOrigin();
        while (i.hasNext()) {
            i.next();
            E dst = i.getDestination();
            if (seen.add(dst)) {
                stack.push(factory.outgoingIterator(dst));
            }
        }
        return (E) origin;
    }

    public void remove() {
        throw new UnsupportedOperationException("Method remove() not supported.");
    }
}
