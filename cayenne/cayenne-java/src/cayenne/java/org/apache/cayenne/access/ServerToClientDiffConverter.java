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

package org.apache.cayenne.access;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.graph.CompoundDiff;
import org.apache.cayenne.graph.GraphChangeHandler;
import org.apache.cayenne.graph.GraphDiff;
import org.apache.cayenne.graph.NodeIdChangeOperation;
import org.apache.cayenne.map.EntityResolver;

/**
 * Converts server-side commit GraphDiff to the client version, accumulating the result in
 * an internal CompoundDiff.
 * 
 * @since 1.2
 * @author Andrus Adamchik
 */
// TODO: currently only supports id change operations. When we can handle all operations,
// this class can be used for pushing any type of synchronization diffs to the client...
class ServerToClientDiffConverter implements GraphChangeHandler {

    EntityResolver resolver;
    CompoundDiff clientDiff;

    ServerToClientDiffConverter(EntityResolver resolver) {
        this.resolver = resolver;
        this.clientDiff = new CompoundDiff();
    }

    GraphDiff getClientDiff() {
        return clientDiff;
    }

    /**
     * Does nothing.
     */
    public void graphCommitAborted() {
    }

    /**
     * Does nothing.
     */
    public void graphCommitStarted() {
    }

    /**
     * Does nothing.
     */
    public void graphCommitted() {
    }

    /**
     * Does nothing.
     */
    public void graphRolledback() {
    }

    public void nodeIdChanged(Object nodeId, Object newId) {
        clientDiff.add(new NodeIdChangeOperation(nodeId, newId));
    }

    public void nodeCreated(Object nodeId) {
        throw new CayenneRuntimeException("Unimplemented...");
    }

    public void nodeRemoved(Object nodeId) {
        throw new CayenneRuntimeException("Unimplemented...");
    }

    public void nodePropertyChanged(
            Object nodeId,
            String property,
            Object oldValue,
            Object newValue) {
        throw new CayenneRuntimeException("Unimplemented...");
    }

    public void arcCreated(Object nodeId, Object targetNodeId, Object arcId) {
        throw new CayenneRuntimeException("Unimplemented...");
    }

    public void arcDeleted(Object nodeId, Object targetNodeId, Object arcId) {
        throw new CayenneRuntimeException("Unimplemented...");
    }
}
