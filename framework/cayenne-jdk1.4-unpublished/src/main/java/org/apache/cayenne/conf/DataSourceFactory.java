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

package org.apache.cayenne.conf;

import javax.sql.DataSource;

/**
 * A pluggable factory for javax.sql.DataSource instances used by Cayenne runtime.
 */
public interface DataSourceFactory {

    /**
     * Initializes factory with the parent configuration object.
     */
    public void initializeWithParentConfiguration(Configuration conf);

    /**
     * Returns DataSource object corresponding to <code>location</code>. Concrete
     * implementations may treat location differently - as a file path, JNDI location,
     * etc.
     */
    public DataSource getDataSource(String location) throws Exception;
}
