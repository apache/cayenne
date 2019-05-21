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
package org.apache.cayenne.swing.components.textpane.syntax;

import java.awt.Color;
import java.awt.Font;

public abstract class SyntaxConstant {

    public static final Font DEFAULT_FONT;
    static {
        String fontName = System.getProperty("os.name").toLowerCase().contains("win")
                ? "Courier New" : "Courier";
        DEFAULT_FONT = new Font(fontName, Font.PLAIN, 14);
    }

    public static final Color DEFAULT_COLOR = Color.black;
    public static final String COMMENT_TEXT = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";
    public static final String COMMENT_TEXT_START = "/\\*.?";
    public static final String STRING_TEXT = "'[^']*'";
    public static final String NUMBER_TEXT = "\\d+";

    public abstract String[] getKEYWORDS();

    public abstract String[] getKEYWORDS2();

    public abstract String[] getTYPES();

    public abstract String[] getOPERATORS();

    public abstract String getContentType();

}
