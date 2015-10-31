package com.progstack.websocket.grizzly;

import org.glassfish.grizzly.filterchain.FilterChainBuilder;
import org.glassfish.grizzly.http.server.AddOn;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServerFilter;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.http.server.ServerConfiguration;

public class WebSocketOverRestAddOn implements AddOn {

    private HttpHandler handler;
    private ServerConfiguration serverConfiguration;

    public WebSocketOverRestAddOn(HttpHandler handler, ServerConfiguration serverConfiguration) {
        this.handler = handler;
        this.serverConfiguration = serverConfiguration;
    }

    private long timeout = 15 * 60;

    /**
     * {@inheritDoc}
     */
    public void setup(NetworkListener networkListener, FilterChainBuilder builder) {
        // Get the index of HttpServerFilter in the HttpServer filter chain
        final int httpServerFilterIdx = builder.indexOfType(HttpServerFilter.class);

        if (httpServerFilterIdx >= 0) {
            // Insert the WebSocketFilter right after HttpServerFilter
            builder.add(httpServerFilterIdx, createWebSocketFilter());
        }
    }

    public long getTimeoutInSeconds() {
        return timeout;
    }

    public void setTimeoutInSeconds(long timeout) {
        this.timeout = timeout;
    }

    protected WebSocketOverRestFilter createWebSocketFilter() {
        return new WebSocketOverRestFilter(timeout, handler, serverConfiguration);
    }
}
