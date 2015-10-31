package com.progstack;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;

import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.HttpServer;
import org.glassfish.grizzly.http.server.NetworkListener;
import org.glassfish.grizzly.websockets.WebSocketApplication;
import org.glassfish.grizzly.websockets.WebSocketEngine;
import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpContainer;
import org.glassfish.jersey.server.ContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import com.ning.http.client.AsyncHttpClient;
import com.ning.http.client.ws.WebSocket;
import com.ning.http.client.ws.WebSocketTextListener;
import com.ning.http.client.ws.WebSocketUpgradeHandler;
import com.progstack.rs.JsonMessageBodyReader;
import com.progstack.rs.JsonMessageBodyWriter;
import com.progstack.websocket.grizzly.WebSocketOverRestAddOn;

public class WebSocketOverRestTest extends Assert {

    private int portNum = 9000;
    private String contextPath = "/progstack";

    private int allocatePort() {
        int pn = portNum;
        while (pn < 6500) {
            try {
                ServerSocket sock = new ServerSocket(pn);
                sock.close();
            } catch (IOException ex) {
                pn++;
            }
        }
        return pn;
    }

    @Before
    public void setUp() throws Exception {
        portNum = allocatePort();
        // register REST services
        final ResourceConfig rc = new ResourceConfig().packages("com.progstack.rs");
        rc.register(JsonMessageBodyWriter.class);
        rc.register(JsonMessageBodyReader.class);
        HttpHandler handler = ContainerFactory.createContainer(GrizzlyHttpContainer.class, rc);
        HttpServer server = HttpServer.createSimpleServer("http://0.0.0.0", 9000);
        server.getServerConfiguration().addHttpHandler(handler, contextPath);

        WebSocketOverRestAddOn socketRestAddOn = new WebSocketOverRestAddOn(handler,
                                                                            server.getServerConfiguration());
        socketRestAddOn.setTimeoutInSeconds(60);
        for (NetworkListener listener : server.getListeners()) {
            listener.registerAddOn(socketRestAddOn);
        }

        // register websocket endpoint
        WebSocketEngine.getEngine().register("", contextPath, new WebSocketApplication() {
        });

        // start it
        server.start();
    }

    @Test
    public void testRestService() {
        assertTrue(false);
    }

    @Test
    public void testWebSocketService() throws InterruptedException, ExecutionException {
        String targetUrl = "";
        AsyncHttpClient client = new AsyncHttpClient();
        final CountDownLatch latch = new CountDownLatch(1);
        WebSocket websocket = client.prepareGet(targetUrl)
            .execute(new WebSocketUpgradeHandler.Builder().addWebSocketListener(new WebSocketTextListener() {

                public void onMessage(String message) {
                }

                public void onOpen(WebSocket websocket) {
                    websocket.sendMessage("");
                }

                public void onClose(WebSocket websocket) {
                    latch.countDown();
                }

                public void onError(Throwable t) {
                }
            }).build()).get();

        websocket.sendMessage("GET " + contextPath + "customers/123");
        client.close();
        assertTrue(false);
    }
}
