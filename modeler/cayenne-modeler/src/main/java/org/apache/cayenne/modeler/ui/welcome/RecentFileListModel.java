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

import org.apache.cayenne.modeler.ui.welcome.path.PathTrimmer;

import javax.swing.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

class RecentFileListModel extends AbstractListModel<String> {

    private final List<File> fileListFull;
    private final List<String> fileList;

    private static final PathTrimmer pathTrimmer = PathTrimmer.getInstance();

    RecentFileListModel(List<File> fileList) {
        this.fileListFull = fileList;
        this.fileList = new ArrayList<>(fileList.size());
        for (File next : fileList) {
            this.fileList.add(pathTrimmer.trim(next.getAbsolutePath()));
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
}
