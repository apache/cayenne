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
package org.apache.cayenne.velocity;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import org.apache.velocity.context.InternalContextAdapter;
import org.apache.velocity.exception.MethodInvocationException;
import org.apache.velocity.exception.ParseErrorException;
import org.apache.velocity.exception.ResourceNotFoundException;
import org.apache.velocity.runtime.Renderable;

/**
 * A parameter value container that helps to may a single velocity variable to a
 * sequence of positional parameters.
 * 
 * @since 4.0
 */
class VelocityParamSequence implements Renderable {

	private List<Object> parameters;
	private int index;

	VelocityParamSequence() {
		this.parameters = new ArrayList<Object>();
	}

	void add(Object parameter) {
		parameters.add(parameter);
	}

	Object next() {
		return parameters.get(index++);
	}

	@Override
	public boolean render(InternalContextAdapter context, Writer writer) throws IOException, MethodInvocationException,
			ParseErrorException, ResourceNotFoundException {

		// rewind the list regardless of whether we produce any output
		Object next = next();

		if (context.getAllowRendering()) {
			writer.write(String.valueOf(next));
		}
		return true;
	}

}