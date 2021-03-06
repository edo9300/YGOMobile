/*
 * ====================================================================
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 */

package org.apache.http.HC4.impl.client;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import org.apache.http.HC4.HttpException;
import org.apache.http.HC4.HttpHost;
import org.apache.http.HC4.HttpRequest;
import org.apache.http.HC4.annotation.ThreadSafe;
import org.apache.http.HC4.client.config.RequestConfig;
import org.apache.http.HC4.client.methods.CloseableHttpResponse;
import org.apache.http.HC4.client.methods.Configurable;
import org.apache.http.HC4.client.methods.HttpExecutionAware;
import org.apache.http.HC4.client.methods.HttpRequestWrapper;
import org.apache.http.HC4.client.protocol.HttpClientContext;
import org.apache.http.HC4.conn.ClientConnectionManager;
import org.apache.http.HC4.conn.HttpClientConnectionManager;
import org.apache.http.HC4.conn.ManagedClientConnection;
import org.apache.http.HC4.impl.execchain.MinimalClientExec;
import org.apache.http.HC4.util.Args;
import org.apache.http.HC4.client.ClientProtocolException;
import org.apache.http.HC4.conn.ClientConnectionRequest;
import org.apache.http.HC4.conn.routing.HttpRoute;
import org.apache.http.HC4.conn.scheme.SchemeRegistry;
import org.apache.http.HC4.impl.DefaultConnectionReuseStrategy;
import org.apache.http.HC4.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.HC4.params.BasicHttpParams;
import org.apache.http.HC4.params.HttpParams;
import org.apache.http.HC4.protocol.BasicHttpContext;
import org.apache.http.HC4.protocol.HttpContext;
import org.apache.http.HC4.protocol.HttpRequestExecutor;

/**
 * Internal class.
 *
 * @since 4.3
 */
@ThreadSafe
@SuppressWarnings("deprecation")
class MinimalHttpClient extends CloseableHttpClient {

    private final HttpClientConnectionManager connManager;
    private final MinimalClientExec requestExecutor;
    private final HttpParams params;

    public MinimalHttpClient(
            final HttpClientConnectionManager connManager) {
        super();
        this.connManager = Args.notNull(connManager, "HTTP connection manager");
        this.requestExecutor = new MinimalClientExec(
                new HttpRequestExecutor(),
                connManager,
                DefaultConnectionReuseStrategy.INSTANCE,
                DefaultConnectionKeepAliveStrategy.INSTANCE);
        this.params = new BasicHttpParams();
    }

    @Override
    protected CloseableHttpResponse doExecute(
            final HttpHost target,
            final HttpRequest request,
            final HttpContext context) throws IOException, ClientProtocolException {
        Args.notNull(target, "Target host");
        Args.notNull(request, "HTTP request");
        HttpExecutionAware execAware = null;
        if (request instanceof HttpExecutionAware) {
            execAware = (HttpExecutionAware) request;
        }
        try {
            final HttpRequestWrapper wrapper = HttpRequestWrapper.wrap(request);
            final HttpClientContext localcontext = HttpClientContext.adapt(
                context != null ? context : new BasicHttpContext());
            final HttpRoute route = new HttpRoute(target);
            RequestConfig config = null;
            if (request instanceof Configurable) {
                config = ((Configurable) request).getConfig();
            }
            if (config != null) {
                localcontext.setRequestConfig(config);
            }
            return this.requestExecutor.execute(route, wrapper, localcontext, execAware);
        } catch (final HttpException httpException) {
            throw new ClientProtocolException(httpException);
        }
    }

    @Override
    public HttpParams getParams() {
        return this.params;
    }

    @Override
    public void close() {
        this.connManager.shutdown();
    }

    @Override
    public ClientConnectionManager getConnectionManager() {

        return new ClientConnectionManager() {

            @Override
            public void shutdown() {
                connManager.shutdown();
            }

            @Override
            public ClientConnectionRequest requestConnection(
                    final HttpRoute route, final Object state) {
                throw new UnsupportedOperationException();
            }

            @Override
            public void releaseConnection(
                    final ManagedClientConnection conn,
                    final long validDuration, final TimeUnit timeUnit) {
                throw new UnsupportedOperationException();
            }

            @Override
            public SchemeRegistry getSchemeRegistry() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void closeIdleConnections(final long idletime, final TimeUnit tunit) {
                connManager.closeIdleConnections(idletime, tunit);
            }

            @Override
            public void closeExpiredConnections() {
                connManager.closeExpiredConnections();
            }

        };

    }

}
