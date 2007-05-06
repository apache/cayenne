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


package org.apache.cayenne.tools.pdf;

import java.awt.Color;

import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;

/**
 * A set of fonts which document use.
 * 
 * @author Anton Sakalouski
 */
public interface PDFFonts {

    public static final Font sectionFont = FontFactory.getFont(
            "Arial",
            20,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font subsectionFont = FontFactory.getFont(
            "Arial",
            14,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font regularFont = FontFactory.getFont(
            FontFactory.TIMES,
            12,
            Font.NORMAL,
            new Color(0, 0, 0));
    public static final Font boldFont = FontFactory.getFont(
            FontFactory.TIMES,
            12,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font italicFont = FontFactory.getFont(
            FontFactory.TIMES,
            12,
            Font.ITALIC,
            new Color(0, 0, 0));
    public static final Font titleFont = FontFactory.getFont(
            FontFactory.TIMES,
            48,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font copyrightFont = FontFactory.getFont(
            FontFactory.TIMES,
            16,
            Font.NORMAL,
            new Color(0, 0, 0));
    public static final Font errorFont = FontFactory.getFont(
            FontFactory.TIMES,
            12,
            Font.NORMAL,
            new Color(255, 0, 0));
    public static final Font sourceFont = FontFactory.getFont(
            FontFactory.COURIER,
            10,
            Font.NORMAL,
            new Color(0, 0, 0));
    public static final Font sourceItalicFont = FontFactory.getFont(
            FontFactory.COURIER,
            10,
            Font.ITALIC,
            new Color(0, 0, 0));
    public static final Font sourceBoldFont = FontFactory.getFont(
            FontFactory.COURIER,
            10,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font sourceBoldItalicFont = FontFactory.getFont(
            FontFactory.COURIER,
            10,
            Font.BOLDITALIC,
            new Color(0, 0, 0));
    public static final Font panelBoldFont = FontFactory.getFont(
            FontFactory.TIMES,
            11,
            Font.BOLD,
            new Color(0, 0, 0));
    public static final Font invisibleFont = FontFactory.getFont(
            FontFactory.TIMES,
            1,
            Font.NORMAL,
            new Color(255, 255, 255));
    public static final Font boldItalicFont = FontFactory.getFont(
            FontFactory.TIMES,
            12,
            Font.BOLDITALIC,
            new Color(0, 0, 0));
}
