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

package org.apache.cayenne.access.jdbc;

import java.io.IOException;
import java.io.Writer;

import org.apache.velocity.context.InternalContextAdapter;

/**
 * A custom Velocity directive to create a PreparedStatement parameter text
 * for "= ?". If null value is encountered, generated text will look like "IS NULL".
 * Usage in Velocity template is "WHERE SOME_COLUMN #bindEqual($xyz)".
 * 
 * @since 1.1
 */
public class BindEqualDirective extends BindDirective {

    @Override
    public String getName() {
        return "bindEqual";
    }

    @Override
    protected void render(
        InternalContextAdapter context,
        Writer writer,
        ParameterBinding binding)
        throws IOException {

        if (binding.getValue() != null) {
            bind(context, binding);
            writer.write("= ?");
        }
        else {
            writer.write("IS NULL");
        }
    }
}
