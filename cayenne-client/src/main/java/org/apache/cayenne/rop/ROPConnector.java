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

import java.io.IOException;
import java.io.InputStream;

/**
 * ROP network connectivity interface.
 * 
 * @since 4.0
 */
public interface ROPConnector {

	/**
	 * Establishes a dedicated session with Cayenne DataChannel, returning session id.
	 */
    InputStream establishSession() throws IOException;

	/**
	 * Creates a new session with the specified or joins an existing one. This method is
	 * used to bootstrap collaborating clients of a single "group chat".
	 */
    InputStream establishSharedSession(String sharedSessionName) throws IOException;

	/**
	 * Processes message on a remote server, returning the result of such processing.
	 */
    InputStream sendMessage(byte[] message) throws IOException;

	/**
	 * Close all resources related to ROP Connector.
	 */
	void close() throws IOException;
    
}
