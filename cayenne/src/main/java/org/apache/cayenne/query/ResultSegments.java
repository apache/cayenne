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

import java.util.List;

/**
 * A query result set mapping. Describes how a result set row is split into segments, and whether a result row is
 * an Object[] with one element per segment, or a single object of the only segment.
 *
 * @param segments result set segments in the order of their columns
 * @param array    whether a result row is an Object[]. If false, there must be exactly one segment
 * @see QueryMetadata#getResultSegments()
 * @since 5.0
 */
public record ResultSegments(List<ResultSegment> segments, boolean array) {

    public ResultSegments {
        if (!array && segments.size() != 1) {
            throw new IllegalArgumentException(
                    "A non-array result must have exactly one segment, got " + segments.size());
        }
    }
}
