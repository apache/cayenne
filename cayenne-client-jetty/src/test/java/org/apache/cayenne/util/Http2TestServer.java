/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * https://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.util;

import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.HttpConfiguration;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.function.BiConsumer;

public class Http2TestServer {

    private final String path;
    private final Server server;
    private final ServerConnector connector;

    public static TestServerBuilder servlet(HttpServlet servlet) {
        return new TestServerBuilder(servlet, "/", 0);
    }

    public static TestServerBuilder handler(BiConsumer<HttpServletRequest, HttpServletResponse> handler) {

        HttpServlet servlet = new HttpServlet() {
            private static final long serialVersionUID = -7741340028518626628L;

            @Override
            protected void service(HttpServletRequest req, HttpServletResponse resp) {
                handler.accept(req, resp);
            }
        };

        return servlet(servlet);
    }


    public Http2TestServer(HttpServlet servlet, String path, int port) {
        this.path = path;

        QueuedThreadPool serverExecutor = new QueuedThreadPool();
        serverExecutor.setName("server");

        server = new Server(serverExecutor);
        connector = new ServerConnector(server, 1, 1, new HTTP2ServerConnectionFactory(new HttpConfiguration()));
        connector.setPort(port);
        server.addConnector(connector);

        ServletContextHandler context = new ServletContextHandler(server, "/", true, false);
        context.addServlet(new ServletHolder(servlet), path);
    }

    void start() {
        try {
            server.start();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            server.stop();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public int getLocalPort() {
        return connector.getLocalPort();
    }


    public String getBasePath() {
        return "http://localhost:" + getLocalPort() + path;
    }


    public static class TestServerBuilder {
        private final HttpServlet servlet;
        private final String path;
        private final int port;

        private TestServerBuilder(HttpServlet servlet, String path, int port) {
            this.servlet = servlet;
            this.path = path;
            this.port = port;
        }

        public TestServerBuilder path(String path) {
            return new TestServerBuilder(this.servlet, path, this.port);
        }

        public TestServerBuilder port(int port) {
            return new TestServerBuilder(this.servlet, this.path, port);
        }


        public Http2TestServer start() {
            Http2TestServer http2Server = new Http2TestServer(servlet, path, port);
            http2Server.start();
            return http2Server;
        }
    }
}
