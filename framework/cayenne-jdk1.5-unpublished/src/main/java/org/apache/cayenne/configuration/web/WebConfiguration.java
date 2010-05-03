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
package org.apache.cayenne.configuration.web;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;

import javax.servlet.FilterConfig;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.apache.cayenne.di.Module;
import org.apache.cayenne.util.Util;

/**
 * A class that provides access to common Cayenne web configuration parameters retrieved
 * either from a FilterConfig or a ServletConfig configuration.
 * 
 * @since 3.1
 */
public class WebConfiguration {

    static final String CONFIGURATION_LOCATION_PARAMETER = "configuration-location";
    static final String EXTRA_MODULES_PARAMETER = "extra-modules";

    private FilterConfig configuration;

    public WebConfiguration(final ServletConfig servletConfiguration) {
        this.configuration = new FilterConfig() {

            public ServletContext getServletContext() {
                return servletConfiguration.getServletContext();
            }

            @SuppressWarnings("all")
            public Enumeration getInitParameterNames() {
                return servletConfiguration.getInitParameterNames();
            }

            public String getInitParameter(String name) {
                return servletConfiguration.getInitParameter(name);
            }

            public String getFilterName() {
                return servletConfiguration.getServletName();
            }
        };
    }

    public WebConfiguration(FilterConfig filterConfiguration) {
        this.configuration = filterConfiguration;
    }

    /**
     * Returns a non-null location of an XML Cayenne configuration, extracted from the
     * filter or servlet configuration parameters.
     */
    public String getConfigurationLocation() {
        String configurationLocation = configuration
                .getInitParameter(CONFIGURATION_LOCATION_PARAMETER);

        if (configurationLocation != null) {
            return configurationLocation;
        }

        String name = configuration.getFilterName();

        if (name == null) {
            return null;
        }

        if (!name.endsWith(".xml")) {
            name = name + ".xml";
        }

        return name;
    }

    /**
     * Creates and returns a collection of modules made of provided standard modules and
     * extra custom modules specified via an optional "extra-modules" init parameter. The
     * value of the parameter is expected to be a comma or space-separated list of class
     * names, with each class implementing {@link Module} interface. Each custom module
     * must have a no-arg constructor. If a module of this type is already in the modules
     * collection, such module is skipped.
     */
    public Collection<Module> createModules(Module... standardModules)
            throws ServletException {

        Set<String> existingModules = new HashSet<String>();
        Collection<Module> modules = new ArrayList<Module>();

        if (standardModules != null) {
            for (Module module : standardModules) {
                modules.add(module);
                existingModules.add(module.getClass().getName());
            }
        }

        String extraModules = configuration.getInitParameter(EXTRA_MODULES_PARAMETER);
        if (extraModules != null) {

            StringTokenizer toks = new StringTokenizer(extraModules, ", \n\r");
            while (toks.hasMoreTokens()) {
                String moduleName = toks.nextToken();

                if (!existingModules.add(moduleName)) {
                    continue;
                }

                Module module;
                try {
                    module = (Module) Util.getJavaClass(moduleName).newInstance();
                }
                catch (Exception e) {
                    String message = String
                            .format(
                                    "Error instantiating custom DI module '%s' by filter '%s': %s",
                                    moduleName,
                                    getClass().getName(),
                                    e.getMessage());
                    throw new ServletException(message, e);
                }

                modules.add(module);
            }
        }

        return modules;
    }

    /**
     * Returns a map of all init parameters from the underlying FilterConfig or
     * ServletConfig object.
     */
    public Map<String, String> getParameters() {
        Enumeration<?> en = configuration.getInitParameterNames();

        if (!en.hasMoreElements()) {
            return Collections.EMPTY_MAP;
        }

        Map<String, String> parameters = new HashMap<String, String>();
        while (en.hasMoreElements()) {
            String key = (String) en.nextElement();
            parameters.put(key, configuration.getInitParameter(key));
        }

        return parameters;
    }

    /**
     * Returns servlet or filter init parameters, excluding those recognized by
     * WebConfiguration. Namely 'configuration-location' and 'extra-modules' parameters
     * are removed from the returned map.
     */
    public Map<String, String> getOtherParameters() {

        Map<String, String> parameters = getParameters();

        if (!parameters.isEmpty()) {
            parameters.remove(CONFIGURATION_LOCATION_PARAMETER);
            parameters.remove(EXTRA_MODULES_PARAMETER);
        }

        return parameters;
    }
}
