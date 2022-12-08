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
package org.apache.cayenne.gen.xml;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.configuration.xml.DataChannelMetaData;
import org.apache.cayenne.gen.CgenConfiguration;
import org.apache.cayenne.gen.CgenConfigList;
import org.apache.cayenne.gen.internal.Utils;
import org.apache.cayenne.map.DataMap;
import org.apache.cayenne.project.extension.BaseSaverDelegate;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * @since 4.1
 */
public class CgenSaverDelegate extends BaseSaverDelegate {

    private DataChannelMetaData metaData;

    CgenSaverDelegate(DataChannelMetaData metaData) {
        this.metaData = metaData;
    }

    @Override
    public Void visitDataMap(DataMap dataMap) {
        CgenConfigList cgenConfigList = metaData.get(dataMap, CgenConfigList.class);
        if (cgenConfigList != null) {
            for (CgenConfiguration cgen : cgenConfigList.getAll()) {
                if (cgen != null) {
                    resolveOutputDir(getBaseDirectory().getURL(), cgen);
                    encoder.nested(cgen, getParentDelegate());
                }
            }
        }
        return null;
    }

    static void resolveOutputDir(URL baseURL, CgenConfiguration cgenConfiguration) {
        if(baseURL == null) {
            return;
        }

        Path baseDirectory = getBaseDirectoryForURL(baseURL);
        Path prevRootPath = cgenConfiguration.getRootPath();
        Path prevOutputPath = cgenConfiguration.buildOutputPath();
        // Update cgen root path.
        cgenConfiguration.setRootPath(baseDirectory);

        // If no root path was set, try to calculate if we are inside Maven tree structure and use it
        if(prevRootPath == null) {
            Utils.getMavenSrcPathForPath(baseDirectory)
                    .map(Path::of)
                    .ifPresent(cgenConfiguration::updateOutputPath);
        }

        if(prevOutputPath != null) {
            // Update relative path to match with the new root
            cgenConfiguration.updateOutputPath(prevOutputPath);
        } else if(cgenConfiguration.buildOutputPath() == null) {
            // No path was set, and we are not in the Maven tree.
            // Set output dir match with the root, nothing else we could do here.
            cgenConfiguration.updateOutputPath(baseDirectory);
        }
    }

    private static Path getBaseDirectoryForURL(URL baseURL) {
        Path resourcePath;
        try {
            resourcePath = Paths.get(baseURL.toURI());
        } catch (URISyntaxException e) {
            throw new CayenneRuntimeException("Unable to resolve output path", e);
        }
        if(Files.isRegularFile(resourcePath)) {
            resourcePath = resourcePath.getParent();
        }
        return resourcePath;
    }
}
