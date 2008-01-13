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

import java.io.ByteArrayInputStream;
import java.io.InputStream;

import junit.framework.Assert;

/**
 * Test setup for a certain domain configuration.
 */
public class ConfigLoaderCase {

    protected int failedMaps;
    protected int failedDataSources;
    protected int failedAdapters;
    protected int failedMapRefs;
    protected int totalDomains;
    protected String configInfo;

    /** Evaluates test case built from this object state. */
    public void test(ConfigLoader loader) throws Exception {
        InputStream in = new ByteArrayInputStream(configInfo.getBytes());
        loader.loadDomains(in);
        RuntimeLoadDelegate delegate = (RuntimeLoadDelegate) loader.getDelegate();
        Assert.assertEquals(totalDomains, delegate.getDomains().size());
        Assert.assertEquals(failedMaps, delegate.getStatus().getFailedMaps().size());
        Assert.assertEquals(failedDataSources, delegate
                .getStatus()
                .getFailedDataSources()
                .size());
        Assert.assertEquals(failedAdapters, delegate
                .getStatus()
                .getFailedAdapters()
                .size());
        Assert
                .assertEquals(failedMapRefs, delegate
                        .getStatus()
                        .getFailedMapRefs()
                        .size());
    }

    public void setTotalDomains(int totalDomains) {
        this.totalDomains = totalDomains;
    }

    public void setConfigInfo(String configInfo) {
        this.configInfo = configInfo;
    }

    public void setFailedMaps(int failedMaps) {
        this.failedMaps = failedMaps;
    }

    public void setFailedDataSources(int failedDataSources) {
        this.failedDataSources = failedDataSources;
    }

    public void setFailedAdapters(int failedAdapters) {
        this.failedAdapters = failedAdapters;
    }

    public void setFailedMapRefs(int failedMapRefs) {
        this.failedMapRefs = failedMapRefs;
    }

    @Override
    public String toString() {
        StringBuffer buf = new StringBuffer();
        buf.append("\n===== DomainHelperCase ====\n").append(configInfo);

        return buf.toString();
    }
}
