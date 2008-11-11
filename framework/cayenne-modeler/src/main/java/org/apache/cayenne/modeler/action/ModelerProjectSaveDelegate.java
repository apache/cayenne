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

package org.apache.cayenne.modeler.action;

import org.apache.cayenne.conf.Configuration;
import org.apache.cayenne.conf.RuntimeSaveDelegate;
import org.apache.cayenne.dba.DbAdapter;
import org.apache.cayenne.modeler.util.ModelerDbAdapter;

/**
 * A custom SaveDelegate that adds some special handling when saving is done from the
 * Modeler.
 * 
 */
class ModelerProjectSaveDelegate extends RuntimeSaveDelegate {

    ModelerProjectSaveDelegate(Configuration config) {
        super(config);
    }

    /**
     * Handles saving adapter name for CustomDbAdapter that is only used within Modeler.
     */
    public String nodeAdapterName(String domainName, String nodeName) {
        DbAdapter adapter = findNode(domainName, nodeName).getAdapter();
        if (adapter instanceof ModelerDbAdapter) {
            ModelerDbAdapter customAdapter = (ModelerDbAdapter) adapter;
            return customAdapter.getAdapterClassName();
        }

        return super.nodeAdapterName(domainName, nodeName);
    }
}
