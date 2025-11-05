package com.example.petzaniasystemtests.utils;

import org.jetbrains.annotations.NotNull;
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.util.concurrent.TimeUnit;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class WebSocketTestClient {
    private final WebSocketStompClient stompClient;
    private StompSession stompSession;
    private final BlockingQueue<Object> blockingQueue;

    public WebSocketTestClient() {
        this.blockingQueue = new LinkedBlockingQueue<>();
        this.stompClient = new WebSocketStompClient(
                new SockJsClient(
                        Collections.singletonList(new WebSocketTransport(new StandardWebSocketClient()))
                )
        );
        this.stompClient.setMessageConverter(new MappingJackson2MessageConverter());
    }

    public void connect(String url, String token) throws Exception {
        StompSessionHandler sessionHandler = new StompSessionHandlerAdapter() {
            @Override
            public void handleException(@NotNull StompSession session, StompCommand command,
                                        @NotNull StompHeaders headers, byte @NotNull [] payload, Throwable exception) {
                exception.printStackTrace();
            }

            @Override
            public void afterConnected(@NotNull StompSession session, @NotNull StompHeaders connectedHeaders) {
                System.out.println("WebSocket Connected!");
            }
        };

        WebSocketHttpHeaders handshakeHeaders = new WebSocketHttpHeaders();
        handshakeHeaders.add("Authorization", "Bearer " + token);

        this.stompSession = stompClient
                .connectAsync(url, handshakeHeaders, sessionHandler)
                .get(5, TimeUnit.SECONDS);
    }

    public void subscribe(String destination, Class<?> messageType) {
        stompSession.subscribe(destination, new StompFrameHandler() {
            @Override
            public Type getPayloadType(@NotNull StompHeaders headers) {
                return messageType;
            }

            @Override
            public void handleFrame(@NotNull StompHeaders headers, Object payload) {
                blockingQueue.add(payload);
            }
        });
    }

    public Object receiveMessage(long timeout, TimeUnit unit) throws InterruptedException {
        return blockingQueue.poll(timeout, unit);
    }

    public void disconnect() {
        if (stompSession != null && stompSession.isConnected()) {
            stompSession.disconnect();
        }
    }
}
