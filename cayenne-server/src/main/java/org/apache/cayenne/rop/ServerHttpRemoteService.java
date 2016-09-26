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
package org.apache.cayenne.rop;

import org.apache.cayenne.configuration.Constants;
import org.apache.cayenne.configuration.ObjectContextFactory;
import org.apache.cayenne.di.Inject;
import org.apache.cayenne.remote.service.HttpRemoteService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.util.Map;

public class ServerHttpRemoteService extends HttpRemoteService {
	
	public ServerHttpRemoteService(@Inject ObjectContextFactory contextFactory,
								   @Inject(Constants.SERVER_ROP_EVENT_BRIDGE_PROPERTIES_MAP) Map<String, String> eventBridgeProperties) {
		super(contextFactory, eventBridgeProperties);
	}

	@Override
	protected HttpSession getSession(boolean create) {
		HttpServletRequest request = (HttpServletRequest) ROPRequestContext.getContextRequest();
		if (request == null) {
			throw new IllegalStateException(
					"Attempt to access HttpSession outside the request scope.");
		}

		return request.getSession(create);
	}
}
