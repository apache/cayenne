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

package org.apache.cayenne.query;

/**
 * A hierarchical visitor interface for traversing a tree of PrefetchTreeNodes. If any of
 * the processing methods return false, node's children will be skipped from traversal.
 *
 * @since 1.2
 * @see org.apache.cayenne.query.PrefetchTreeNode#traverse(PrefetchProcessor)
 */
public interface PrefetchProcessor {

    boolean startPhantomPrefetch(PrefetchTreeNode node);

    boolean startDisjointPrefetch(PrefetchTreeNode node);

    /**
     * @since 3.1
     */
    boolean startDisjointByIdPrefetch(PrefetchTreeNode prefetchTreeNode);

    boolean startJointPrefetch(PrefetchTreeNode node);

    boolean startUnknownPrefetch(PrefetchTreeNode node);

    void finishPrefetch(PrefetchTreeNode node);
}
