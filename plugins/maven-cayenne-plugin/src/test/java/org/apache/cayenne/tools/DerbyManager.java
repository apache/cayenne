/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.apache.cayenne.tools;

/**
 * @since 4.0
 */

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DerbyManager {

    public static final OutputStream DEV_NULL = new OutputStream() {

        @Override
        public void write(int b) {
        }
    };

    DerbyManager(String location) {

        System.setProperty("derby.stream.error.field", DerbyManager.class.getName() + ".DEV_NULL");

        File derbyDir = new File(location);
        if (derbyDir.isDirectory()) {
            try {
                FileUtils.deleteDirectory(derbyDir);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    void shutdown() {
        try {
            DriverManager.getConnection("jdbc:derby:;shutdown=true");
        } catch (SQLException e) {
            // the exception is actually expected on shutdown... go figure...
        }
    }
}
