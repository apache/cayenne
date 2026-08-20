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

package org.apache.cayenne.modeler;

import org.apache.cayenne.modeler.service.os.OperatingSystem;
import org.apache.cayenne.modeler.platform.UIInitializer;

public final class Main {

    public static void main(String[] args) {
        Application.launch(args, loadPlatformInitializer(OperatingSystem.os));
    }

    static UIInitializer loadPlatformInitializer(OperatingSystem os) {
        String fqn = os == OperatingSystem.MAC_OS
            ? "org.apache.cayenne.modeler.platform.mac.MacUIInitializer"
            : "org.apache.cayenne.modeler.platform.GenericUIInitializer";

        try {
            return (UIInitializer) Class.forName(fqn).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load platform initializer " + fqn, e);
        }
    }
}
