/*****************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ****************************************************************/

package org.apache.cayenne.rop.http2;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.eclipse.jetty.http2.api.Stream;
import org.eclipse.jetty.http2.frames.DataFrame;
import org.eclipse.jetty.http2.frames.HeadersFrame;
import org.eclipse.jetty.http2.frames.PushPromiseFrame;
import org.eclipse.jetty.http2.frames.ResetFrame;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.util.Callback;

import java.io.IOException;
import java.io.OutputStream;


public class LowHttp2ROPResponseListener implements Stream.Listener {

    private static Log logger = LogFactory.getLog(LowHttp2ROPResponseListener.class);

    private OutputStream outputStream;

    public LowHttp2ROPResponseListener(OutputStream stream) {
        this.outputStream = stream;
    }

    @Override
    public void onHeaders(Stream stream, HeadersFrame frame) {
    }

    @Override
    public Stream.Listener onPush(Stream stream, PushPromiseFrame frame) {
        return null;
    }

    @Override
    public void onData(Stream stream, DataFrame frame, Callback callback) {
        try {
            callback.succeeded();

            outputStream.write(BufferUtil.toArray(frame.getData()));

            if (frame.isEndStream()) {
                outputStream.flush();
                outputStream.close();
            }
        } catch (IOException e) {
            callback.failed(e);
        }
    }

    @Override
    public void onReset(Stream stream, ResetFrame frame) {
        logger.error("Stream has been canceled: " + frame);
    }

    @Override
    public void onTimeout(Stream stream, Throwable x) {
        logger.error(x.getMessage(), x);
    }

}