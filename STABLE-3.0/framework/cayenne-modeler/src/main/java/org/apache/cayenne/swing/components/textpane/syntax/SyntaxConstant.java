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
package org.apache.cayenne.swing.components.textpane.syntax;

import java.awt.Color;
import java.awt.Font;

public interface SyntaxConstant {

    public static Font DEFAULT_FONT = new Font("Courier", Font.PLAIN, 14);
    public static Color DEFAULT_COLOR = Color.black;
    public static String COMMENT_TEXT = "(?:/\\*(?:[^*]|(?:\\*+[^*/]))*\\*+/)|(?://.*)";
    public static final String COMMENT_TEXT_START = "/\\*.?";
    public static String STRING_TEXT = "'[^']*'";
    public static String NUMBER_TEXT = "\\d+";

    public String[] getKEYWORDS();

    public String[] getKEYWORDS2();

    public String[] getTYPES();

    public String[] getOPERATORS();
    
    public String getContentType();

}
