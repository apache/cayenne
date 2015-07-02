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

package org.apache.cayenne.event;

import java.util.Collection;
import java.util.Map;

/**
 * Factory to create JavaGroupsBridge instances. If JavaGroups library is not installed
 * this factory will return a noop EventBridge as a failover mechanism.
 * <p/>
 * For further information about JavaGroups consult the <a href="http://www.jgroups.org/">documentation</a>.
 *
 * @since 1.1
 */
public class JavaGroupsBridgeFactory implements EventBridgeFactory {

    /**
     * Creates a JavaGroupsBridge instance. Since JavaGroups is not shipped with Cayenne
     * and should be installed separately, a common misconfiguration problem may be the
     * absence of JavaGroups jar file. This factory returns a dummy noop EventBridge, if
     * this is the case. This would allow the application to continue to run, but without
     * remote notifications.
     */
    public EventBridge createEventBridge(
            Collection<EventSubject> localSubjects,
            String externalSubject,
            Map<String, String> properties) {
        return new JavaGroupsBridge(localSubjects, externalSubject, properties);
    }

}
