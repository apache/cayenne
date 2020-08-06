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
package org.apache.cayenne.rop;

import org.apache.cayenne.CayenneRuntimeException;
import org.apache.cayenne.di.BeforeScopeEnd;
import org.apache.cayenne.event.EventBridge;
import org.apache.cayenne.event.EventBridgeFactory;
import org.apache.cayenne.remote.BaseConnection;
import org.apache.cayenne.remote.ClientMessage;
import org.apache.cayenne.remote.RemoteService;
import org.apache.cayenne.remote.RemoteSession;

import java.rmi.RemoteException;

public class HttpClientConnection extends BaseConnection {

	private RemoteService remoteService;
	private RemoteSession session;

	private String sharedSessionName;
    
    public HttpClientConnection(RemoteService remoteService, String sharedSession) {
        this.remoteService = remoteService;
        this.sharedSessionName = sharedSession;
    }

    public RemoteSession getSession() {
        return session;
    }

	@Override
	protected void beforeSendMessage(ClientMessage message) throws CayenneRuntimeException {
		if (session == null) {
			connect();
		}
	}

	@Override
	protected Object doSendMessage(ClientMessage message) throws CayenneRuntimeException {
        try {
            return remoteService.processMessage(message);
        }
        catch (CayenneRuntimeException e) {
            throw e;
        }
        catch (Throwable th) {
            throw new CayenneRuntimeException(th.getMessage(), th);
        }
	}

	@Override
	public EventBridge getServerEventBridge() throws CayenneRuntimeException {
        if (session == null) {
            connect();
        }

        return createServerEventBridge(session);
	}

    @BeforeScopeEnd
    public void shutdown() throws RemoteException {
            remoteService.close();
    }

	protected synchronized void connect() {
		if (session != null) {
			return;
		}
        
        long t0 = System.currentTimeMillis();

		// create server session...
		try {
			this.session = (sharedSessionName != null) ? remoteService
					.establishSharedSession(sharedSessionName) : remoteService
					.establishSession();
		}
		catch (Throwable th) {
			logger.info(th.getMessage(), th);
			throw new CayenneRuntimeException(th.getMessage(), th);
		}

        if (logger.isInfoEnabled()) {
            long time = System.currentTimeMillis() - t0;
            logger.info("=== Connected, session: "
                    + session
                    + " - took "
                    + time
                    + " ms.");
        }
	}

    /**
     * Creates an EventBridge that will listen for server events. Returns null if server
     * events support is not configured in the descriptor.
     *
     * @throws CayenneRuntimeException if EventBridge startup fails for any reason.
     */
    protected EventBridge createServerEventBridge(RemoteSession session) throws CayenneRuntimeException {

        if (!session.isServerEventsEnabled()) {
            return null;
        }

        try {
            EventBridgeFactory factory = (EventBridgeFactory) Class.forName(session.getEventBridgeFactory())
                    .newInstance();

            // must use "name", not the sessionId as an external subject for the
            // event bridge
            return factory.createEventBridge(RemoteSession.getSubjects(), session.getName(),
                    session.getEventBridgeParameters());
        } catch (Exception ex) {
            throw new CayenneRuntimeException("Error creating EventBridge.", ex);
        }
    }
}
