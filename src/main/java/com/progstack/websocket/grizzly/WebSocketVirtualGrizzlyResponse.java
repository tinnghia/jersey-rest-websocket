package com.progstack.websocket.grizzly;

import java.io.IOException;
import java.io.OutputStream;

import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.websockets.WebSocket;

public class WebSocketVirtualGrizzlyResponse extends Response {

    private WebSocket webSocket;

    public WebSocketVirtualGrizzlyResponse(WebSocket webSocket) {
        this.webSocket = webSocket;
    }

    @Override
    public OutputStream getOutputStream() {
        return new WebSocketOutputStream(webSocket);
    }

    private static class WebSocketOutputStream extends OutputStream {

        private WebSocket webSocket;

        public WebSocketOutputStream(WebSocket webSocket) {
            this.webSocket = webSocket;
        }

        @Override
        public void write(int data) throws IOException {
            if (webSocket.isConnected()) {
                webSocket.send(String.valueOf(data));
            }
        }

        @Override
        public void write(byte[] data) throws IOException {
            if (webSocket.isConnected()) {
                webSocket.send(data);
            }
        }

        @Override
        public void write(byte[] data, int off, int length) throws IOException {

            byte[] bytes = new byte[length];
            System.arraycopy(data, off, bytes, 0, length);
            write(bytes);
        }
    }

}
