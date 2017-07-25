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

package org.apache.cayenne.project.extension;

/**
 * <p>DataMap XML file extension mechanics.</p>
 * <p>
 *     Can be used to enhance datamap.map.xml files with additional (really random) information.
 *     By default extensions not used by {@link org.apache.cayenne.configuration.server.ServerRuntime} or
 *     ClientRuntime so they can safely store big chunks of data.
 * </p>
 * <p>
 *     Extensions can be contributed by {@link org.apache.cayenne.project.ProjectModule#contributeExtension(org.apache.cayenne.di.Binder)}.
 *     {@link org.apache.cayenne.project.ProjectModule} currently used by Modeler and cli tools, e.g. cdbimport and cgen.
 * </p>
 *
 * @see org.apache.cayenne.project.extension.info.InfoExtension as reference implementation
 * @since 4.1
 */
public interface ProjectExtension {

    /**
     * @return delegate that handle loading phase of XML processing
     */
    LoaderDelegate createLoaderDelegate();

    /**
     * @return delegate that handle saving phase of XML processing
     */
    SaverDelegate createSaverDelegate();

}
