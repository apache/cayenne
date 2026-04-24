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

package org.apache.cayenne.modeler.ui.welcome;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class RecentFileListModel extends AbstractListModel<String> {

    private static final int MAX_LENGTH = 120;
    private static final String homeDir = System.getProperty("user.home");
    private final static String replacement;

    static {
        replacement = homeDir.endsWith(File.separator) ? "~" + File.separator : "~";
    }

    private final List<File> fileListFull;
    private final List<String> fileList;

    RecentFileListModel(List<File> fileList) {
        this.fileListFull = fileList;
        this.fileList = new ArrayList<>(fileList.size());
        for (File next : fileList) {
            this.fileList.add(trim(next.getAbsolutePath()));
        }
    }

    @Override
    public int getSize() {
        return fileList.size();
    }

    @Override
    public String getElementAt(int index) {
        return fileList.get(index);
    }

    File getFullElementAt(int index) {
        return fileListFull.get(index);
    }

    private static String trim(String path) {
        String t1 = path.replace(homeDir, replacement);
        return t1.length() <= MAX_LENGTH ? t1 : "..." + path.substring(path.length() - MAX_LENGTH);
    }
}
