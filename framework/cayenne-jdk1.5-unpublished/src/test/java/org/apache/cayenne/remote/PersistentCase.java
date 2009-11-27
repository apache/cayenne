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
package org.apache.cayenne.remote;

import org.apache.cayenne.ObjectContext;

/**
 * Test for entites that are implemented in same class on client and server
 */
public class PersistentCase extends RemoteCayenneCase {
    
    boolean server;
    
    @Override
    public void runBare() throws Throwable {
        server = true;
        super.runBare();
        server = false;
        //testing ROP with all serialozation policies
        runBareSimple();
    }
    
    protected ObjectContext createContext() {
        if (server) {
            return createDataContext();
        }
        else {
            return createROPContext();
        }
    }
}
