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

import java.util.Map;

import org.apache.cayenne.map.DataMap;

/**
 * Interface that defines callback API used by ConfigLoader to process loaded
 * configuration. Main responsibility of ConfigLoaderDelegate is to create objects, while
 * ConfigLoader is mainly concerned with XML parsing.
 */
public interface ConfigLoaderDelegate {

    /**
     * Callback methods invoked in the beginning of the configuration processing.
     */
    public void startedLoading();

    /**
     * Callback methods invoked at the end of the configuration processing.
     */
    public void finishedLoading();

    /**
     * Callback method invoked when a project version is read.
     * 
     * @since 1.1
     */
    public void shouldLoadProjectVersion(String version);

    /**
     * Callback method invoked when a domain is encountered in the configuration file.
     * 
     * @param name domain name.
     */
    public void shouldLoadDataDomain(String name);

    /**
     * Callback method invoked when a DataView reference is encountered in the
     * configuration file.
     * 
     * @since 1.1
     */
    public void shouldRegisterDataView(String name, String location);

    /**
     * @since 1.1
     */
    public void shouldLoadDataMaps(String domainName, Map<String, DataMap> locations);

    /**
     * @since 1.1
     */
    public void shouldLoadDataDomainProperties(
            String domainName,
            Map<String, String> properties);

    public void shouldLoadDataNode(
            String domainName,
            String nodeName,
            String dataSource,
            String adapter,
            String factory,
            String schemaUpdateStrategy);

    public void shouldLinkDataMap(String domainName, String nodeName, String mapName);

    /**
     * Gives delegate an opportunity to process the error.
     * 
     * @param th
     * @return boolean indicating whether ConfigLoader should proceed with further
     *         processing. Ultimately it is up to the ConfigLoader to make this decision.
     */
    public boolean loadError(Throwable th);

    /**
     * @return status object indicating the state of the configuration loading.
     */
    public ConfigStatus getStatus();
}
