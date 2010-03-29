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

package org.apache.cayenne.pref;

import java.io.File;

/**
 */
public class HSQLEmbeddedPreferenceEditor extends CayennePreferenceEditor {

    protected Delegate delegate;

    public HSQLEmbeddedPreferenceEditor(HSQLEmbeddedPreferenceService service) {
        super(service);
    }

    public Delegate getDelegate() {
        return delegate;
    }

    public void setDelegate(Delegate delegate) {
        this.delegate = delegate;
    }

    protected void restart() {
        try {
            service.stopService();
            checkForLocks();
            service.startService();
        }
        finally {
            restartRequired = false;
        }
    }

    protected HSQLEmbeddedPreferenceService getHSQLService() {
        return (HSQLEmbeddedPreferenceService) getService();
    }

    protected boolean checkForLocks() {
        if (delegate != null) {
            HSQLEmbeddedPreferenceService service = getHSQLService();
            if (service.isSecondaryDB()) {
                File lock = service.getMasterLock();
                if (lock.isFile()) {
                    return delegate.deleteMasterLock(lock);
                }
            }
        }

        return true;
    }

    // delegate interface allowing UI to interfere with the editor tasks
    public static interface Delegate {

        boolean deleteMasterLock(File lock);
    }
}
