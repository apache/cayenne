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

package org.apache.cayenne.modeler;

import com.jgoodies.looks.plastic.PlasticXPLookAndFeel;

/**
 * Defines constants used in the modeler.
 * 
 */
public interface ModelerConstants {

    /** Defines path to the images. */
    public static final String RESOURCE_PATH = "org/apache/cayenne/modeler/images/";

    public static final String DEFAULT_MESSAGE_BUNDLE = "org.apache.cayenne.modeler.cayennemodeler-strings";

    public static final String TITLE = "CayenneModeler";
    public static final String DEFAULT_LAF_NAME = PlasticXPLookAndFeel.class.getName();

    // note that previous default - "Desert Blue" theme doesn't support Chinese and
    // Japanese chars
    public static final String DEFAULT_THEME_NAME = "Sky Bluer";

}
