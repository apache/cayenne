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

package org.apache.cayenne.gen;

import org.apache.cayenne.map.Commentable;
import org.apache.cayenne.project.ProjectNode;

/**
 * Exposes the comments of the mapping objects to the class generation templates as "$metadataUtils".
 *
 * @since 4.2
 */
public class MetadataUtils {

    private static final String COMMENT_KEY = "comment";

    public String getComment(ProjectNode node) {
        return node instanceof Commentable commentable ? commentable.getComment() : null;
    }

    /**
     * @deprecated since 6.0 in favor of {@link #getComment(ProjectNode)}, as comments are the only supported info
     */
    @Deprecated(since = "6.0", forRemoval = true)
    public String getInfo(ProjectNode node, String key) {
        return COMMENT_KEY.equals(key) ? getComment(node) : null;
    }
}
