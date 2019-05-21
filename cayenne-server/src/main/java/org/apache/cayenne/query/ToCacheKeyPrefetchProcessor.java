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

package org.apache.cayenne.query;

/**
 * Prefetch processor that append prefetch tree into cache key.
 * @since 4.2
 */
class ToCacheKeyPrefetchProcessor implements PrefetchProcessor {

    private final StringBuilder out;

    ToCacheKeyPrefetchProcessor(StringBuilder out) {
        this.out = out;
    }

    @Override
    public boolean startPhantomPrefetch(PrefetchTreeNode node) {
        return true;
    }

    @Override
    public boolean startDisjointPrefetch(PrefetchTreeNode node) {
        out.append("/pd:").append(node.getPath());
        return true;
    }

    @Override
    public boolean startDisjointByIdPrefetch(PrefetchTreeNode node) {
        out.append("/pi:").append(node.getPath());
        return true;
    }

    @Override
    public boolean startJointPrefetch(PrefetchTreeNode node) {
        out.append("/pj:").append(node.getPath());
        return true;
    }

    @Override
    public boolean startUnknownPrefetch(PrefetchTreeNode node) {
        out.append("/pu:").append(node.getPath());
        return true;
    }

    @Override
    public void finishPrefetch(PrefetchTreeNode node) {
    }
}
