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
import org.apache.cayenne.query.ObjectSelect;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;

/**
 * A handler of the "objectQuery" tag of a DataMap. The query is defined by a single String in the syntax of
 * {@link ObjectSelect#parse(String, Object...)}.
 *
 * @since 5.0
 */
public class SelectQueryDescriptorHandler extends QueryDescriptorHandler {

    static final String OBJECT_QUERY_TAG = "objectQuery";
    private static final String QL_TAG = "ql";

    private SelectQueryDescriptor descriptor;

    public SelectQueryDescriptorHandler(NamespaceAwareNestedTagHandler parentHandler, DataMap map) {
        super(parentHandler, map);
    }

    @Override
    public SelectQueryDescriptor getQueryDescriptor() {
        return descriptor;
    }

    @Override
    protected boolean processElement(String namespaceURI, String localName, Attributes attributes) throws SAXException {
        return switch (localName) {
            case OBJECT_QUERY_TAG -> {
                descriptor = new SelectQueryDescriptor();
                loadQuerySettings(descriptor, attributes);
                yield true;
            }
            case QL_TAG, CACHE_GROUP_TAG -> true;
            default -> false;
        };
    }

    @Override
    protected boolean processCharData(String localName, String data) {
        switch (localName) {
            case QL_TAG -> setSelect(data);
            case CACHE_GROUP_TAG -> descriptor.setCacheGroup(data);
        }
        return true;
    }

    /**
     * Sets the query from its String form. The String is not parsed here: a String that can't be parsed is kept as is,
     * and fails when the query is run, or the project is validated.
     */
    private void setSelect(String select) {
        if (!select.isBlank()) {
            descriptor.setQl(select);
        }
    }
}
