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
package org.apache.cayenne.access.select;

import java.util.List;

import org.apache.cayenne.access.types.ExtendedType;
import org.apache.cayenne.access.types.ExtendedTypeMap;
import org.apache.cayenne.query.ScalarResultSegment;

/**
 * @since 3.0
 */
class ScalarSegmentBuilder {

    private List<Object> resultDescriptors;
    private ExtendedType converter;

    ScalarSegmentBuilder(ExtendedTypeMap extendedTypes, List<Object> resultDescriptors) {
        this.converter = extendedTypes.getDefaultType();
        this.resultDescriptors = resultDescriptors;
    }

    SelectDescriptor<Object> getSegment(int position) {
        ScalarResultSegment segment = (ScalarResultSegment) resultDescriptors
                .get(position);
        return new ScalarSegment(segment.getColumn(), converter);
    }
}
