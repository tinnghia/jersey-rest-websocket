package com.progstack.websocket.grizzly;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Collection;

import org.glassfish.grizzly.Buffer;
import org.glassfish.grizzly.Connection;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.filterchain.NextAction;
import org.glassfish.grizzly.http.HttpContent;
import org.glassfish.grizzly.http.HttpHeader;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.HttpResponsePacket;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpHandlerRegistration;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.ServerConfiguration;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.websockets.BaseWebSocketFilter;
import org.glassfish.grizzly.websockets.ClosingFrame;
import org.glassfish.grizzly.websockets.DataFrame;
import org.glassfish.grizzly.websockets.FramingException;
import org.glassfish.grizzly.websockets.HandshakeException;
import org.glassfish.grizzly.websockets.WebSocket;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.grizzly.websockets.WebSocketException;
import org.glassfish.grizzly.websockets.WebSocketHolder;

public class WebSocketOverRestFilter extends BaseWebSocketFilter {
    private HttpHandler handler;
    private ServerConfiguration serverConfiguration;

    public WebSocketOverRestFilter(HttpHandler handler, ServerConfiguration serverConfiguration) {
        super();
        this.handler = handler;
        this.serverConfiguration = serverConfiguration;
    }

    public WebSocketOverRestFilter(long wsTimeoutInSeconds, HttpHandler handler,
                                   ServerConfiguration serverConfiguration) {
        super(wsTimeoutInSeconds);
        this.handler = handler;
        this.serverConfiguration = serverConfiguration;
    }

    @Override
    public NextAction handleRead(FilterChainContext ctx) throws IOException {
        // Get the Grizzly Connection
        final Connection connection = ctx.getConnection();
        // Get the parsed HttpContent (we assume prev. filter was HTTP)
        final HttpContent message = ctx.getMessage();
        // Get the HTTP header
        final HttpHeader header = message.getHttpHeader();
        // Try to obtain associated WebSocket
        final WebSocketHolder holder = WebSocketHolder.get(connection);
        WebSocket ws = WebSocketHolder.getWebSocket(connection);

        if (ws == null || !ws.isConnected()) {
            // If websocket is null - it means either non-websocket Connection,
            // or websocket with incomplete handshake
            if (!webSocketInProgress(connection) && !"websocket".equalsIgnoreCase(header.getUpgrade())) {
                // if it's not a websocket connection - pass the processing to
                // the next filter
                return ctx.getInvokeAction();
            }

            try {
                // Handle handshake
                return handleHandshake(ctx, message);
            } catch (HandshakeException e) {
                e.printStackTrace();

                onHandshakeFailure(connection, e);
            }
            // Handshake error
            return ctx.getStopAction();
        }
        if (message.getContent().hasRemaining()) {
            // get the frame(s) content

            Buffer buffer = message.getContent();
            message.recycle();
            // check if we're currently parsing a frame
            try {
                while (buffer != null && buffer.hasRemaining()) {
                    if (holder.buffer != null) {
                        buffer = Buffers.appendBuffers(ctx.getMemoryManager(), holder.buffer, buffer);

                        holder.buffer = null;
                    }
                    final DataFrame result = holder.handler.unframe(buffer);
                    if (result == null) {
                        holder.buffer = buffer;
                        break;
                    } else {
                        result.respond(holder.webSocket);
                    }
                }

                // delegate to REST endpoints

                WebSocketRestPayload restPayload = new WebSocketRestPayload(holder.buffer.toString());

                Request handlerRequest = Request.create();
                HttpRequestPacket.Builder requestbuilder = new HttpRequestPacket.Builder();
                requestbuilder.method(restPayload.getMethod());
                requestbuilder.uri(restPayload.getRestUri());
                requestbuilder.protocol("HTTP/1.1");
                HttpRequestPacket requestPacket = requestbuilder.build();
                requestPacket.setConnection(connection);
                requestPacket.getRequestURIRef().init(restPayload.getRestUri());

                HttpResponsePacket.Builder responsebuilder = new HttpResponsePacket.Builder();
                responsebuilder.requestPacket(requestPacket);
                final HttpResponsePacket responsePacket = responsebuilder.build();

                final Response handlerResponse = handlerRequest.getResponse();
                handlerRequest.initialize(requestPacket, ctx, null);

                WebSocketVirtualGrizzlyResponse virtualResponse = new WebSocketVirtualGrizzlyResponse(ws);

                handlerResponse.initialize(handlerRequest, responsePacket, ctx, null, null);
                virtualResponse.initialize(handlerRequest, responsePacket, ctx, null, null);

                // set context path
                Method setCtx = Request.class.getDeclaredMethod("setContextPath", String.class);
                setCtx.setAccessible(true);
                setCtx.invoke(handlerRequest, getContextPath(restPayload.getRestUri()));

                handler.service(handlerRequest, virtualResponse);

                return ctx.getStopAction();
            } catch (FramingException e) {
                holder.webSocket.onClose(new ClosingFrame(e.getClosingCode(), e.getMessage()));
            } catch (Exception wse) {
                holder.webSocket.onClose(new ClosingFrame(1011, wse.getMessage()));
            }
        }
        return ctx.getStopAction();
    }

    // ---------------------------------------- Methods from BaseWebSocketFilter

    @Override
    protected NextAction handleHandshake(FilterChainContext ctx, HttpContent content) throws IOException {
        return handleServerHandshake(ctx, content);
    }

    // --------------------------------------------------------- Private Methods

    /**
     * Handle server-side websocket handshake
     * 
     * @param ctx {@link FilterChainContext}
     * @param requestContent HTTP message
     * @throws {@link IOException}
     */
    private NextAction handleServerHandshake(final FilterChainContext ctx, final HttpContent requestContent)
        throws IOException {

        // get HTTP request headers
        final HttpRequestPacket request = (HttpRequestPacket)requestContent.getHttpHeader();
        try {
            if (doServerUpgrade(ctx, requestContent)) {
                return ctx.getInvokeAction(); // not a WS request, pass to the next filter.
            }
            setIdleTimeout(ctx);
        } catch (HandshakeException e) {
            ctx.write(composeHandshakeError(request, e));
            throw e;
        }
        requestContent.recycle();

        return ctx.getStopAction();

    }

    protected boolean doServerUpgrade(final FilterChainContext ctx, final HttpContent requestContent)
        throws IOException {
        return !WebSocketEngine.getEngine().upgrade(ctx, requestContent);
    }

    private static HttpResponsePacket composeHandshakeError(final HttpRequestPacket request,
                                                            final HandshakeException e) {
        final HttpResponsePacket response = request.getResponse();
        response.setStatus(e.getCode());
        response.setReasonPhrase(e.getMessage());
        return response;
    }

    private String getContextPath(String restUri) {
        Collection<HttpHandlerRegistration[]> mappings = serverConfiguration.getHttpHandlersWithMapping()
            .values();
        for (HttpHandlerRegistration[] regs : mappings) {
            for (HttpHandlerRegistration reg : regs) {
                if (restUri.startsWith(reg.getContextPath())) {
                    return reg.getContextPath();
                }
            }
        }
        return "";
    }

    private static class WebSocketRestPayload {
        private String payload;
        private String method;
        private String restUri;

        WebSocketRestPayload(String payload) {
            this.payload = payload;
            processInternal();
        }

        private void processInternal() {
            if (payload.startsWith("GET ")) {
                method = "GET";
                restUri = payload.substring("GET ".length());
            } else if (payload.startsWith("POST ")) {
                method = "POST";
                restUri = payload.substring("POST ".length());
            } else if (payload.startsWith("PUT ")) {
                method = "PUT";
                restUri = payload.substring("PUT ".length());
            } else if (payload.startsWith("PATCH ")) {
                method = "PATCH";
                restUri = payload.substring("PATCH ".length());
            } else if (payload.startsWith("DELETE ")) {
                method = "DELETE";
                restUri = payload.substring("DELETE ".length());
            } else {
                throw new WebSocketException("Unsupported method");
            }
        }

        public String getMethod() {
            return method;
        }

        public String getRestUri() {
            return restUri;
        }
    }

}
