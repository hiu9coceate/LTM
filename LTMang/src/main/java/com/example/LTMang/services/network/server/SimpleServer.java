package com.example.LTMang.services.network.server;

import org.java_websocket.server.WebSocketServer;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.json.JSONObject;
import java.net.InetSocketAddress;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class SimpleServer extends WebSocketServer {
    private Set<WebSocket> conns = Collections.synchronizedSet(new HashSet<>());

    public SimpleServer(int port) {
        super(new InetSocketAddress(port));
    }

    @Override
    public void onOpen(WebSocket conn, ClientHandshake handshake) {
        conns.add(conn);
        System.out.println(">> [SERVER] New connection: " + conn.getRemoteSocketAddress());
    }

    @Override
    public void onClose(WebSocket conn, int code, String reason, boolean remote) {
        conns.remove(conn);
    }

    @Override
    public void onMessage(WebSocket conn, String message) {
        
        try {
            new JSONObject(message);
            
            broadcast(message); 
        } catch (Exception e) {
            
        }
    }

    @Override public void onError(WebSocket conn, Exception ex) {}
    @Override public void onStart() {
        System.out.println("⚡ TỔNG ĐÀI (SIGNALING SERVER) ĐANG CHẠY TRÊN PORT: " + getPort());
    }
}

