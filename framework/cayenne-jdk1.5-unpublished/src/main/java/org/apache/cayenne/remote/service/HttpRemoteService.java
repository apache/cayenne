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

package org.apache.cayenne.remote.service;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpSession;

import org.apache.cayenne.DataChannel;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.remote.RemoteSession;

/**
 * A {@link org.apache.cayenne.remote.RemoteService} implementation that stores server
 * context information in HTTP sessions.
 * 
 * @since 1.2
 */
public abstract class HttpRemoteService extends BaseRemoteService {

    static final String SESSION_ATTRIBUTE = HttpRemoteService.class.getName()
            + ".ServerSession";

    private Map<String, WeakReference<DataChannel>> sharedChannels;

    /**
     * @since 3.1
     */
    public HttpRemoteService(ObjectContextFactory contextFactory,
            Map<String, String> eventBridgeProperties) {
        super(contextFactory, eventBridgeProperties);
        this.sharedChannels = new HashMap<String, WeakReference<DataChannel>>();
    }

    /**
     * Returns an HttpSession associated with the current request in progress.
     */
    protected abstract HttpSession getSession(boolean create);

    /**
     * Returns a ServerSession object that represents Cayenne-related state associated
     * with the current session. If ServerSession hasn't been previously saved, returns
     * null.
     */
    @Override
    protected ServerSession getServerSession() {
        HttpSession httpSession = getSession(true);
        return (ServerSession) httpSession.getAttribute(SESSION_ATTRIBUTE);
    }

    /**
     * Creates a new ServerSession with a dedicated DataChannel. Returned ServerSession is
     * stored in HttpSession for future reuse.
     */
    @Override
    protected ServerSession createServerSession() {

        HttpSession httpSession = getSession(true);

        DataChannel channel = createChannel();
        RemoteSession remoteSession = createRemoteSession(
                httpSession.getId(),
                null,
                false);
        ServerSession serverSession = new ServerSession(remoteSession, channel);

        httpSession.setAttribute(SESSION_ATTRIBUTE, serverSession);
        return serverSession;
    }

    /**
     * Creates a new ServerSession based on a shared DataChannel. Returned ServerSession
     * is stored in HttpSession for future reuse.
     * 
     * @param name shared session name used to lookup a shared DataChannel.
     */
    @Override
    protected ServerSession createServerSession(String name) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null for shared session.");
        }

        HttpSession httpSession = getSession(true);
        DataChannel channel;

        synchronized (sharedChannels) {
            channel = getSharedChannel(name);
            if (channel == null) {
                channel = createChannel();
                saveSharedChannel(name, channel);
                logger.debug("Starting a new shared channel: " + name);
            }
            else {
                logger.debug("Joining existing shared channel: " + name);
            }
        }

        RemoteSession remoteSession = createRemoteSession(httpSession.getId(), name, true);

        ServerSession serverSession = new ServerSession(remoteSession, channel);
        httpSession.setAttribute(SESSION_ATTRIBUTE, serverSession);
        return serverSession;
    }

    protected DataChannel getSharedChannel(String name) {
        WeakReference<DataChannel> ref = sharedChannels.get(name);
        return (ref != null) ? ref.get() : null;
    }

    protected void saveSharedChannel(String name, DataChannel channel) {
        // wrap value in a WeakReference so that channels can be deallocated when all
        // sessions that reference this channel time out...
        sharedChannels.put(name, new WeakReference<DataChannel>(channel));
    }
}
