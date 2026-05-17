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
import org.apache.cayenne.modeler.platform.UIPlatformInitializer;

public final class Main {

    public static void main(String[] args) {
        Application.launch(args, loadPlatformInitializer());
    }

    private static UIPlatformInitializer loadPlatformInitializer() {
        String fqn = switch (OperatingSystem.getOS()) {
            case MAC_OS_X -> "org.apache.cayenne.modeler.platform.osx.OSXPlatformInitializer";
            case WINDOWS  -> "org.apache.cayenne.modeler.platform.win.WinPlatformInitializer";
            case OTHER    -> "org.apache.cayenne.modeler.platform.generic.GenericPlatformInitializer";
        };
        try {
            return (UIPlatformInitializer) Class.forName(fqn).getDeclaredConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException("Cannot load platform initializer " + fqn, e);
        }
    }
}
