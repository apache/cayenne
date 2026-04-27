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
package org.apache.cayenne.modeler.toolkit.text.style;

import org.apache.cayenne.modeler.service.os.OperatingSystem;

import java.awt.Color;
import java.awt.Font;

public interface TextSyntax {

    Font DEFAULT_FONT = new Font(OperatingSystem.getOS() == OperatingSystem.WINDOWS
            ? "Courier New"
            : "Courier", Font.PLAIN, 14);

    Color DEFAULT_COLOR = Color.black;
    String COMMENT_TEXT = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";
    String COMMENT_TEXT_START = "/\\*.?";
    String NUMBER_TEXT = "\\d+";

    String[] keywords();

    String[] keywords2();

    String[] types();

    String[] operators();

    String contentType();
}
