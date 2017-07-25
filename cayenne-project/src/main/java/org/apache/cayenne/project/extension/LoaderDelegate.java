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

import org.apache.cayenne.configuration.xml.NamespaceAwareNestedTagHandler;

/**
 * Delegate that handles loading process for extension specific parts of XML document.
 *
 * @since 4.1
 */
public interface LoaderDelegate {

    /**
     * @return target namespace that this extension is using
     */
    String getTargetNamespace();

    /**
     * Create handler that will handle parsing process further.
     *
     * @param parent parent handler
     * @param tag current tag that in question
     * @return new handler that will process tag or null if there is no interest in tag
     */
    NamespaceAwareNestedTagHandler createHandler(NamespaceAwareNestedTagHandler parent, String tag);

}
