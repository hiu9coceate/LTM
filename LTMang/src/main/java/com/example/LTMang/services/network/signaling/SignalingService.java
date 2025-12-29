package com.example.LTMang.services.network.signaling;

import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import java.net.URI;
import java.util.function.Consumer;

public class SignalingService extends WebSocketClient {
    private String myId;
    private Consumer<String> onSignalListener;

    public SignalingService(URI serverUri, String myId) {
        super(serverUri);
        this.myId = myId;
    }

    public void setOnSignalListener(Consumer<String> listener) {
        this.onSignalListener = listener;
    }

    @Override
    public void onOpen(ServerHandshake handshakedata) {
        
        this.send("{\"type\":\"LOGIN\", \"id\":\"" + myId + "\"}");
    }

    @Override
    public void onMessage(String message) {
        if (onSignalListener != null) {
            onSignalListener.accept(message);
        }
    }

    @Override public void onClose(int code, String reason, boolean remote) {}
    @Override public void onError(Exception ex) { ex.printStackTrace(); }
}

