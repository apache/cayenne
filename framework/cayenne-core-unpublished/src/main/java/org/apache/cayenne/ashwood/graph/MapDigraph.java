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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

/**
 * @since 3.1
 */
public class MapDigraph<E, V> implements Digraph<E, V> {

    private Map<E, Map<E, V>> graph;
    private int size;

    public MapDigraph() {
        graph = new HashMap<E, Map<E, V>>();
    }

    public boolean addVertex(E vertex) {
        if (graph.containsKey(vertex)) {
            return false;
        }

        graph.put(vertex, new HashMap<E, V>());
        return true;
    }

    public boolean addAllVertices(Collection<? extends E> vertices) {
        if (graph.keySet().containsAll(vertices)) {
            return false;
        }

        for (E vertex : vertices) {
            addVertex(vertex);
        }

        return true;
    }

    public V putArc(E origin, E destination, V arc) {
        Map<E, V> destinations = graph.get(origin);
        if (destinations == null) {
            destinations = new HashMap<E, V>();
            graph.put(origin, destinations);
        }

        addVertex(destination);
        V oldArc = destinations.put(destination, arc);
        if (oldArc == null) {
            size++;
        }

        return oldArc;
    }

    public V getArc(Object origin, Object destination) {
        Map<E, V> destinations = graph.get(origin);
        if (destinations == null) {
            return null;
        }

        return destinations.get(destination);
    }

    public boolean removeVertex(E vertex) {
        Map<E, V> destination = graph.remove(vertex);
        if (destination != null)
            size -= destination.size();
        else
            return false;

        removeIncoming(vertex);
        return true;
    }

    public boolean removeAllVertices(Collection<? extends E> vertices) {
        boolean modified = false;

        for (E vertex : vertices) {
            modified |= removeVertex(vertex);
        }

        return modified;
    }

    public Object removeArc(E origin, E destination) {
        Map<E, V> destinations = graph.get(origin);
        if (destinations == null) {
            return null;
        }

        V arc = destinations.remove(destination);
        if (arc != null)
            size--;

        return arc;
    }

    public boolean removeIncoming(E vertex) {
        boolean modified = false;

        for (Map<E, V> destinations : graph.values()) {
            Object arc = destinations.remove(vertex);
            if (arc != null)
                size--;
            modified |= (arc != null);
        }

        return modified;
    }

    public boolean removeOutgoing(E vertex) {

        Map<E, V> destinations = graph.remove(vertex);
        if (destinations != null)
            size -= destinations.size();
        else
            return false;
        boolean modified = !destinations.isEmpty();
        destinations.clear();
        return modified;
    }

    public Iterator<E> vertexIterator() {
        return graph.keySet().iterator();
    }

    public ArcIterator<E, V> arcIterator() {
        return new AllArcIterator();
    }

    public ArcIterator<E, V> outgoingIterator(E vertex) {
        if (!containsVertex(vertex)) {
            return ArcIterator.EMPTY_ITERATOR;
        }

        return new OutgoingArcIterator(vertex);
    }

    public ArcIterator<E, V> incomingIterator(E vertex) {
        if (!containsVertex(vertex))
            return ArcIterator.EMPTY_ITERATOR;
        return new IncomingArcIterator(vertex);
    }

    public int order() {
        return graph.size();
    }

    public int size() {
        return size;
    }

    public int outgoingSize(E vertex) {
        Map<E, V> destinations = graph.get(vertex);
        if (destinations == null)
            return 0;
        else
            return destinations.size();
    }

    public int incomingSize(E vertex) {
        int count = 0;
        if (!graph.containsKey(vertex))
            return 0;

        for (Map<E, V> destinations : graph.values()) {
            count += (destinations.containsKey(vertex) ? 1 : 0);
        }

        return count;
    }

    public boolean containsVertex(E vertex) {
        return graph.containsKey(vertex);
    }

    public boolean containsAllVertices(Collection<? extends E> vertices) {
        return graph.keySet().containsAll(vertices);
    }

    public boolean hasArc(E origin, E destination) {
        Map<E, V> destinations = graph.get(origin);
        if (destinations == null)
            return false;
        return destinations.containsKey(destination);
    }

    public boolean isEmpty() {
        return graph.isEmpty();
    }

    public boolean isOutgoingEmpty(E vertex) {
        return outgoingSize(vertex) == 0;
    }

    public boolean isIncomingEmpty(E vertex) {
        return incomingSize(vertex) == 0;
    }

    private class AllArcIterator implements ArcIterator<E, V> {

        private Iterator<Entry<E, Map<E, V>>> originIterator;
        private Iterator<Entry<E, V>> destinationIterator;
        private E origin, nextOrigin;
        private E destination, nextDst;
        private V arc, nextArc;

        private AllArcIterator() {
            originIterator = graph.entrySet().iterator();
            next();
        }

        public E getOrigin() {
            return origin;
        }

        public E getDestination() {
            return destination;
        }

        public boolean hasNext() {
            return nextArc != null;
        }

        public V next() {
            origin = nextOrigin;
            destination = nextDst;
            arc = nextArc;
            if (destinationIterator == null || !destinationIterator.hasNext()) {
                nextOrigin = null;
                nextDst = null;
                nextArc = null;

                while (originIterator.hasNext()) {
                    Entry<E, Map<E, V>> entry = originIterator.next();
                    destinationIterator = entry.getValue().entrySet().iterator();
                    if (destinationIterator.hasNext()) {
                        nextOrigin = entry.getKey();
                        Entry<E, V> entry1 = destinationIterator.next();
                        nextDst = entry1.getKey();
                        nextArc = entry1.getValue();
                        break;
                    }
                }
            }
            else {
                Entry<E, V> entry1 = destinationIterator.next();
                nextDst = entry1.getKey();
                nextArc = entry1.getValue();
            }

            return arc;
        }

        public void remove() {
            throw new UnsupportedOperationException(
                    "Method remove() not yet implemented.");
        }
    }

    private class OutgoingArcIterator implements ArcIterator<E, V> {

        private E origin;
        private Iterator<Entry<E, V>> dstIt;
        private Entry<E, V> entry;

        private OutgoingArcIterator(E vertex) {
            origin = vertex;
            dstIt = graph.get(vertex).entrySet().iterator();
        }

        public E getOrigin() {
            return origin;
        }

        public E getDestination() {
            if (entry == null)
                return null;
            return entry.getKey();
        }

        public boolean hasNext() {
            return dstIt.hasNext();
        }

        public V next() {
            entry = dstIt.next();
            return entry.getValue();
        }

        public void remove() {
            throw new UnsupportedOperationException(
                    "Method remove() not yet implemented.");
        }
    }

    private class IncomingArcIterator implements ArcIterator<E, V> {

        private E dst;
        private E origin, nextOrigin;
        private V arc, nextArc;
        private Iterator<Entry<E, Map<E, V>>> graphIt;

        private IncomingArcIterator(E vertex) {
            dst = vertex;
            graphIt = graph.entrySet().iterator();
            next();
        }

        public E getOrigin() {
            return origin;
        }

        public E getDestination() {
            return dst;
        }

        public boolean hasNext() {
            return (nextArc != null);
        }

        public V next() {
            origin = nextOrigin;
            arc = nextArc;
            nextArc = null;
            nextOrigin = null;
            while (graphIt.hasNext()) {
                Entry<E, Map<E, V>> entry = graphIt.next();
                Map<E, V> destinations = entry.getValue();
                nextArc = destinations.get(dst);
                if (nextArc != null) {
                    nextOrigin = entry.getKey();
                    break;
                }
            }
            return arc;
        }

        public void remove() {
            throw new java.lang.UnsupportedOperationException(
                    "Method remove() not yet implemented.");
        }
    }

}
