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

package org.apache.cayenne.commitlog;

import org.apache.cayenne.di.Binder;
import org.apache.cayenne.di.ListBuilder;
import org.apache.cayenne.di.Module;
import org.apache.cayenne.commitlog.meta.IncludeAllCommitLogEntityFactory;
import org.apache.cayenne.commitlog.meta.CommitLogEntityFactory;

/**
 * @since 4.0
 */
public class CommitLogModule implements Module{

    public static ListBuilder<CommitLogListener> contributeListeners(Binder binder) {
        return binder.bindList(CommitLogListener.class);
    }

    @Override
    public void configure(Binder binder) {
        contributeListeners(binder);
        binder.bind(CommitLogEntityFactory.class).to(IncludeAllCommitLogEntityFactory.class);
        binder.bind(CommitLogFilter.class).to(CommitLogFilter.class);
    }
}
