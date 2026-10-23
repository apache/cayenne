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

package org.apache.cayenne.project.xml;

import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.map.SelectQueryDescriptor;
import org.apache.cayenne.query.PrefetchTreeNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class PrefetchTypeForSelectQueryHandlerTest extends BaseHandlerTest{

    @Test
    public void load() throws Exception {
        final DataMap map = new DataMap();

        parse("objectQuery", new HandlerFactory() {
            @Override
            public NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent) {
                return new SelectQueryDescriptorHandler(parent, map);
            }
        });

        SelectQueryDescriptor selectQueryDescriptor = (SelectQueryDescriptor) map.getQueryDescriptor("query");
        PrefetchTreeNode prefetches = selectQueryDescriptor.getParsedQuery().getPrefetches();
        assertEquals(3, prefetches.nonPhantomNodes().size());

        assertEquals(1, prefetches.getNode("paintings").getSemantics());
        assertEquals(2, prefetches.getNode("paintings.artist").getSemantics());
        assertEquals(3, prefetches.getNode("paintings.gallery").getSemantics());
    }

}
