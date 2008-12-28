/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.cayenne.modeler.util;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * FileWatchdog is a watcher for files' change. If one of the files has changed or been
 * removed, a {@link #doOnChange(org.apache.cayenne.modeler.util.FileWatchdog.FileInfo)}
 * or {@link #doOnRemove(org.apache.cayenne.modeler.util.FileWatchdog.FileInfo) method}
 * will be called
 * 
 * Original code taken from Log4J project
 * 
 */
public abstract class FileWatchdog extends Thread {

    /**
     * The default delay between every file modification check
     */
    static final public long DEFAULT_DELAY = 4000;

    /**
     * The names of the files to observe for changes.
     */
    protected Map<String, FileInfo> filesInfo;

    /**
     * The delay to observe between every check. By default set {@link #DEFAULT_DELAY}.
     */
    protected long delay = DEFAULT_DELAY;

    /**
     * Paused flag
     */
    protected boolean paused;

    /**
     * This flags shows whether only one or multiple notifications will be fired when
     * several files change
     */
    protected boolean singleNotification;

    /**
     * An object to enable synchronization
     */
    private Object sync = new Object();

    private static Log log = LogFactory.getLog(FileWatchdog.class);

    protected FileWatchdog() {
        filesInfo = Collections.synchronizedMap(new HashMap<String, FileInfo>());
        setDaemon(true);
    }

    /**
     * Sets whether only one or multiple notifications will be fired when several files
     * change
     */
    public void setSingleNotification(boolean b) {
        singleNotification = b;
    }

    /**
     * Returns whether only one or multiple notifications will be fired when several files
     * change
     */
    public boolean isSingleNotification() {
        return singleNotification;
    }

    /**
     * Adds a new file to watch
     * 
     * @param location path of file
     */
    public void addFile(String location) {
        synchronized (sync) {
            try {
                filesInfo.put(location, new FileInfo(location));
            }
            catch (SecurityException e) {
                log.error("SecurityException adding file " + location, e);
            }
        }
    }

    /**
     * Turns off watching for a specified file
     * 
     * @param location path of file
     */
    public void removeFile(String location) {
        synchronized (sync) {
            filesInfo.remove(location);
        }
    }

    /**
     * Turns off watching for all files
     */
    public void removeAllFiles() {
        synchronized (sync) {
            filesInfo.clear();
        }
    }

    /**
     * Set the delay to observe between each check of the file changes.
     */
    public void setDelay(long delay) {
        this.delay = delay;
    }

    /**
     * Invoked when one of the watched files has changed
     * 
     * @param fileInfo Changed file info
     */
    protected abstract void doOnChange(FileInfo fileInfo);

    /**
     * Invoked when one of the watched files has been removed
     * 
     * @param fileInfo Changed file info
     */
    protected abstract void doOnRemove(FileInfo fileInfo);

    protected void check() {
        synchronized (sync) {
            if (paused)
                return;

            List<FileInfo> changed = new Vector<FileInfo>();
            List<FileInfo> deleted = new Vector<FileInfo>();

            for (Iterator<FileInfo> it = filesInfo.values().iterator(); it.hasNext();) {
                FileInfo fi = it.next();

                boolean fileExists;
                try {
                    fileExists = fi.getFile().exists();
                }
                catch (SecurityException e) {
                    log.error(
                            "SecurityException checking file " + fi.getFile().getPath(),
                            e);

                    // we still process with other files
                    continue;
                }

                if (fileExists) {
                    long l = fi.getFile().lastModified(); // this can also throw a
                                                            // SecurityException
                    if (l > fi.getLastModified()) { // however, if we reached this point
                                                    // this
                        fi.setLastModified(l); // is very unlikely.
                        changed.add(fi);
                    }
                }
                else if (fi.getLastModified() != -1) // the file has been removed
                {
                    deleted.add(fi);
                    it.remove(); // no point to watch the file now
                }
            }

            for (FileInfo aDeleted : deleted) {
                doOnRemove(aDeleted);
                if (singleNotification)
                    return;
            }
            for (FileInfo aChanged : changed) {
                doOnChange(aChanged);
                if (singleNotification)
                    return;
            }
        }
    }

    public void run() {
        while (true) {
            try {
                Thread.sleep(delay);
                check();
            }
            catch (InterruptedException e) {
                // someone asked to stop
                return;
            }
        }
    }

    /**
     * Tells watcher to pause watching for some time. Useful before changing files
     */
    public void pauseWatching() {
        synchronized (sync) {
            paused = true;
        }
    }

    /**
     * Resumes watching for files
     */
    public void resumeWatching() {
        paused = false;
    }

    /**
     * Class to store information about files (last modification time & File pointer)
     */
    protected class FileInfo {

        /**
         * Exact java.io.File object, may not be null
         */
        File file;

        /**
         * Time the file was modified
         */
        long lastModified;

        /**
         * Creates new object
         * 
         * @param location the file path
         */
        public FileInfo(String location) {
            file = new File(location);
            lastModified = file.exists() ? file.lastModified() : -1;
        }

        public File getFile() {
            return file;
        }

        public long getLastModified() {
            return lastModified;
        }

        public void setLastModified(long l) {
            lastModified = l;
        }
    }
}
