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

package org.apache.cayenne.dbsync.xml;

import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.dbsync.reverse.dbimport.ReverseEngineering;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

/**
 * @since 4.1
 */
class DbImportSaverDelegate extends BaseSaverDelegate {

    private DataChannelMetaData metaData;

    DbImportSaverDelegate(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        ReverseEngineering reverseEngineering = metaData.get(dataMap, ReverseEngineering.class);
        if(reverseEngineering != null) {
            encoder.nested(reverseEngineering, getParentDelegate());
        }
        return null;
    }
}
