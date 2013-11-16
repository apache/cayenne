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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;

/**
 * @since 3.1
 */
public class IndegreeTopologicalSort<E> implements Iterator<E> {

    private Digraph<E, ?> digraph;
    private List<E> vertices = new LinkedList<E>();
    private Map<E, InDegree> inDegrees = new HashMap<E, InDegree>();
    private ListIterator<E> current;

    public IndegreeTopologicalSort(Digraph<E, ?> digraph) {
        this.digraph = digraph;
        for (Iterator<E> i = digraph.vertexIterator(); i.hasNext();) {
            E vertex = i.next();
            vertices.add(vertex);
            inDegrees.put(vertex, new InDegree(digraph.incomingSize(vertex)));
        }
        current = vertices.listIterator();
    }

    public boolean hasNext() {
        return !vertices.isEmpty();
    }

    public E next() {
        boolean progress = true;
        while (hasNext()) {
            if (!current.hasNext()) {
                if (!progress)
                    break;
                progress = false;
                current = vertices.listIterator();
            }
            E vertex = current.next();
            InDegree indegree = inDegrees.get(vertex);
            if (indegree.value == 0) {
                removeVertex(vertex);
                current.remove();
                return vertex;
            }
        }
        return null;
    }

    private void removeVertex(E vertex) {
        for (ArcIterator<E, ?> i = digraph.outgoingIterator(vertex); i.hasNext();) {
            i.next();
            E dst = i.getDestination();
            InDegree indegree = inDegrees.get(dst);
            indegree.value--;
        }
    }

    public void remove() {
        throw new UnsupportedOperationException("Method remove() not supported.");
    }

    private static class InDegree {

        int value;

        InDegree(int value) {
            InDegree.this.value = value;
        }
    }
}
