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


package org.apache.cayenne.dataview.dvmodeler;

import java.awt.Component;
import java.awt.Graphics;
import javax.swing.Icon;

/**
 *
 * @author Andriy Shapochka
 * @version 1.0
 */

public class EmptyIcon implements Icon {
  public static final Icon DEFAULT_ICON = new EmptyIcon();

  private int width = 1;
  private int height = 1;

  public EmptyIcon() {
  }
  public EmptyIcon(int width, int height) {
    this.width  = width;
    this.height = height;
  }
  public void paintIcon(Component c, Graphics g, int x, int y) {
  }
  public int getIconWidth() {
    return width;
  }
  public int getIconHeight() {
    return height;
  }
}
