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

import java.util.Iterator;

/**
 * Defines a set of callback methods that provide information
 * to ConfigSaver when Cayenne project is saved.
 * 
 */
public interface ConfigSaverDelegate {
    /**
     * @since 1.1
     */
    public String projectVersion();
    
    public Iterator domainNames();

    /**
     * @since 1.1
     */    
    public Iterator<String> viewNames();
    
    /**
     * @since 1.1
     */    
    public String viewLocation(String dataViewName);
    
    public Iterator propertyNames(String domainName);
    
    public String propertyValue(String domainName, String propertyName);
    
    public Iterator mapNames(String domainName);
    
    public String mapLocation(String domainName, String mapName);
    
    public Iterator linkedMapNames(String domainName, String nodeName);
    
    public Iterator nodeNames(String domainName); 
    
    public String nodeDataSourceName(String domainName, String nodeName);
    
    public String nodeAdapterName(String domainName, String nodeName);
    
    public String nodeFactoryName(String domainName, String nodeName);
    
    /**
     * @since 3.0
     */
    public String nodeSchemaUpdateStrategyName(String domainName, String nodeName);
}
