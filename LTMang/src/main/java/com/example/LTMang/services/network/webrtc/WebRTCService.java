package com.example.LTMang.services.network.webrtc;

import dev.onvoid.webrtc.*;
import org.json.JSONObject;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import dev.onvoid.webrtc.media.MediaStream;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class WebRTCService {
    private PeerConnectionFactory factory;
    private RTCPeerConnection peerConnection;
    private final Object channelLock = new Object();
    private volatile RTCDataChannel dataChannel;
    private volatile boolean isInitiator = false;
    private Consumer<String> sendSignalFunc;
    
    private Consumer<byte[]> onImageReceived;
    private Consumer<String> onControlCommand;
    private Runnable onChannelOpen;
    
    private final LinkedBlockingQueue<byte[]> sendQueue = new LinkedBlockingQueue<>(1);
    private volatile boolean senderRunning = false;
    private static final int MAX_CHUNK_SIZE = 16000; 
    private final ByteArrayOutputStream receiveBuffer = new ByteArrayOutputStream();

    public WebRTCService(Consumer<String> sendSignalFunc) {
        this.sendSignalFunc = sendSignalFunc;
        this.factory = new PeerConnectionFactory();
        setup();
    }
    
    public void setOnImageReceived(Consumer<byte[]> listener) { this.onImageReceived = listener; }
    public void setOnControlCommand(Consumer<String> listener) { this.onControlCommand = listener; }
    public void setOnChannelOpen(Runnable listener) { this.onChannelOpen = listener; }

    private void setup() {
        RTCConfiguration config = new RTCConfiguration();
        List<RTCIceServer> servers = new ArrayList<>();

        System.out.println(">> [MODE] Wifi/LAN Mode Activated (Clean Local)");
        
        config.iceServers = servers; 
        
        peerConnection = factory.createPeerConnection(config, new PeerConnectionObserver() {
            @Override public void onIceCandidate(RTCIceCandidate c) {
                if (c.sdp != null) {
                    JSONObject msg = new JSONObject(); 
                    msg.put("type", "candidate"); 
                    msg.put("candidate", c.sdp); 
                    msg.put("sdpMid", c.sdpMid); 
                    msg.put("sdpMLineIndex", c.sdpMLineIndex);
                    sendSignalFunc.accept(msg.toString());

                    System.out.println(">> [CANDIDATE] " + c.sdp);
                }
            }
            
            @Override public void onDataChannel(RTCDataChannel dc) { 
                synchronized(channelLock) { if (isInitiator || dataChannel != null) return; dataChannel = dc; }
                setupCallbacks();
            }
            @Override public void onIceConnectionChange(RTCIceConnectionState s) { System.out.println("P2P State: " + s); }
            @Override public void onSignalingChange(RTCSignalingState s) {}
            @Override public void onIceConnectionReceivingChange(boolean b) {}
            @Override public void onIceGatheringChange(RTCIceGatheringState s) {}
            @Override public void onIceCandidatesRemoved(RTCIceCandidate[] c) {}
            @Override public void onAddStream(MediaStream s) {}
            @Override public void onRemoveStream(MediaStream s) {}
            @Override public void onRenegotiationNeeded() {}
            @Override public void onAddTrack(RTCRtpReceiver r, MediaStream[] s) {}
            @Override public void onTrack(RTCRtpTransceiver t) {}
        });
    }

    private void setupCallbacks() {
        if (dataChannel == null) return;
        dataChannel.registerObserver(new RTCDataChannelObserver() {
            @Override public void onStateChange() { 
                if (dataChannel.getState() == RTCDataChannelState.OPEN) { 
                    if (onChannelOpen != null) onChannelOpen.run(); 
                    startSender(); 
                } 
            }
            
            @Override public void onMessage(RTCDataChannelBuffer buffer) {
                try {
                    if (buffer.binary) {
                        byte[] data = new byte[buffer.data.remaining()];
                        buffer.data.get(data);
                        synchronized(receiveBuffer) { receiveBuffer.write(data); }
                    } else {
                        byte[] bytes = new byte[buffer.data.remaining()];
                        buffer.data.get(bytes);
                        String text = new String(bytes, StandardCharsets.UTF_8);
                        
                        if (text.startsWith("START:")) {
                            synchronized(receiveBuffer) { receiveBuffer.reset(); }
                        } else if (text.equals("END")) {
                            synchronized(receiveBuffer) {
                                byte[] img = receiveBuffer.toByteArray();
                                if (img.length > 0 && onImageReceived != null) onImageReceived.accept(img);
                                receiveBuffer.reset();
                            }
                        } else {
                            if (onControlCommand != null) onControlCommand.accept(text);
                        }
                    }
                } catch (Exception e) {}
            }
            
            @Override public void onBufferedAmountChange(long l) {}
        });
    }

    public void createOffer() {
        isInitiator = true; 
        synchronized(channelLock) { 
            dataChannel = peerConnection.createDataChannel("screen", new RTCDataChannelInit()); 
        }
        setupCallbacks();
        peerConnection.createOffer(new RTCOfferOptions(), new CreateSessionDescriptionObserver() {
            @Override public void onSuccess(RTCSessionDescription sdp) { 
                peerConnection.setLocalDescription(sdp, new SetSessionDescriptionObserver() { 
                    @Override public void onSuccess() { 
                        JSONObject msg = new JSONObject(); 
                        msg.put("type", "offer"); 
                        msg.put("sdp", sdp.sdp); 
                        sendSignalFunc.accept(msg.toString()); 
                    } 
                    @Override public void onFailure(String e) {} 
                }); 
            }
            @Override public void onFailure(String e) {}
        });
    }

    public void handleRemoteAnswer(String sdp) { 
        peerConnection.setRemoteDescription(new RTCSessionDescription(RTCSdpType.ANSWER, sdp), new SetSessionDescriptionObserver() { 
            @Override public void onSuccess() {} 
            @Override public void onFailure(String e) {} 
        }); 
    }
    
    public void addIceCandidate(JSONObject d) { 
        peerConnection.addIceCandidate(new RTCIceCandidate(d.getString("sdpMid"), d.getInt("sdpMLineIndex"), d.getString("candidate"))); 
    }
    
    private void startSender() {
        if (senderRunning) return; senderRunning = true;
        new Thread(() -> {
            while (senderRunning) {
                try {
                    byte[] data = sendQueue.poll(1, TimeUnit.SECONDS);
                    if (data == null) continue;
                    synchronized(channelLock) {
                        if (dataChannel != null && dataChannel.getState() == RTCDataChannelState.OPEN) {
                            try {
                                sendString("START:" + data.length);
                                int offset = 0;
                                while (offset < data.length) {
                                    int chunk = Math.min(MAX_CHUNK_SIZE, data.length - offset);
                                    byte[] buf = new byte[chunk]; 
                                    System.arraycopy(data, offset, buf, 0, chunk);
                                    dataChannel.send(new RTCDataChannelBuffer(ByteBuffer.wrap(buf), true));
                                    offset += chunk;
                                }
                                sendString("END"); 
                                Thread.sleep(20);
                            } catch (Exception e) {}
                        }
                    }
                } catch (Exception e) {}
            }
        }).start();
    }

    public void sendImage(byte[] d) { sendQueue.clear(); sendQueue.offer(d); }
    
    public void sendString(String msg) {
        if (dataChannel != null && dataChannel.getState() == RTCDataChannelState.OPEN) {
            try {
                dataChannel.send(new RTCDataChannelBuffer(ByteBuffer.wrap(msg.getBytes(StandardCharsets.UTF_8)), false));
            } catch (Exception e) {}
        }
    }
}


