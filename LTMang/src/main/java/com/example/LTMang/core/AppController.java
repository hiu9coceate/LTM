package com.example.LTMang.core;

import com.example.LTMang.config.AppConfig;
import com.example.LTMang.services.network.signaling.SignalingService;
import com.example.LTMang.services.network.webrtc.WebRTCService;
import com.example.LTMang.services.media.capture.ScreenCaptureService;
import com.example.LTMang.services.media.processing.ImageProcessor;
import com.example.LTMang.services.media.processing.JavaOCRService;
import com.example.LTMang.services.media.processing.GeminiService; 
import org.json.JSONObject;
import java.net.URI;
import java.awt.Dimension;
import java.awt.image.BufferedImage;

public class AppController {
    static volatile boolean isP2PConnected = false;
    private static String currentControllerId = null;
    private static RemoteRobot remoteRobot;
    private static JavaOCRService ocrService;
    private static GeminiService geminiService; 

    public static void startSharing(String myId) {
        new Thread(() -> {
            try {
                remoteRobot = new RemoteRobot();
                geminiService = new GeminiService(); 
                try {
                    ocrService = new JavaOCRService();
                } catch (Throwable e) {
                    System.err.println("⚠️ Lỗi tải OCR: " + e.getMessage());
                }

                Dimension screenSize = remoteRobot.getScreenSize();
                System.out.println(">> [APP] Connecting...");

                SignalingService client = new SignalingService(new URI(AppConfig.SERVER_URL), myId);
                ScreenCaptureService capturer = new ScreenCaptureService();

                if (client.connectBlocking()) {
                    System.out.println(">> [APP] Connected ID: " + myId);
                    WebRTCService rtcManager = new WebRTCService(msg -> {
                        JSONObject signal = new JSONObject();
                        signal.put("type", "SIGNAL");
                        if (currentControllerId != null) {
                            signal.put("target", currentControllerId);
                            signal.put("data", msg);
                            client.send(signal.toString());
                        }
                    });

                    rtcManager.setOnControlCommand(cmd -> {
                        try {
                            
                            if (cmd.startsWith("OCR_REQ:")) {
                                String coords = cmd.substring(8); 
                                BufferedImage img = cropImageFromStr(coords, screenSize);
                                if (ocrService != null) {
                                    String text = ocrService.performOCR(img);
                                    rtcManager.sendString("OCR_RESULT:" + text);
                                } else
                                    rtcManager.sendString("OCR_RESULT:Lỗi: Server chưa có OCR Model");
                            }

                            else if (cmd.startsWith("AI_REQ:")) {
                                String payload = cmd.substring(7);
                                String[] parts = payload.split("\\|", 2); 
                                String coords = parts[0];
                                String question = (parts.length > 1) ? parts[1] : "";

                                System.out.println("?? AI Request: " + question);

                                BufferedImage img = cropImageFromStr(coords, screenSize);
                                
                                String base64 = ImageProcessor.compress(img);

                                String answer = geminiService.askGemini(base64, question);

                                System.out.println("? AI Reply: "
                                        + (answer.length() > 20 ? answer.substring(0, 20) + "..." : answer));
                                rtcManager.sendString("AI_RESULT:" + answer);
                            }
                            
                            else if (cmd.startsWith("MOUSE:")) {
                                String[] parts = cmd.substring(6).split(",");
                                remoteRobot.moveMouse(Float.parseFloat(parts[0]), Float.parseFloat(parts[1]));
                            } else if (cmd.equals("CLICK")) {
                                remoteRobot.click();
                            } else if (cmd.startsWith("SCROLL:")) {
                                try {
                                    
                                    int notches = Integer.parseInt(cmd.split(":")[1]);

                                    new java.awt.Robot().mouseWheel(notches);

                                } catch (Exception e) {
                                    e.printStackTrace();
                                }
                            }

                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    });

                    rtcManager.setOnChannelOpen(() -> {
                        if (!isP2PConnected) {
                            isP2PConnected = true;
                            new Thread(() -> {
                                while (isP2PConnected) {
                                    try {
                                        byte[] imgBytes = capturer.captureAsByteArray();
                                        if (imgBytes != null)
                                            rtcManager.sendImage(imgBytes);
                                        Thread.sleep(60);
                                    } catch (Exception e) {
                                    }
                                }
                            }).start();
                        }
                    });

                    client.setOnSignalListener(msgStr -> {
                        try {
                            JSONObject json = new JSONObject(msgStr);
                            if (json.has("data")) {
                                String senderId = json.optString("target");
                                JSONObject data = new JSONObject(json.getString("data"));
                                String type = data.optString("type");
                                if ("HELLO".equals(type)) {
                                    currentControllerId = senderId;
                                    if (!isP2PConnected)
                                        rtcManager.createOffer();
                                } else if ("answer".equals(type)) {
                                    rtcManager.handleRemoteAnswer(data.getString("sdp"));
                                } else if ("candidate".equals(type)) {
                                    rtcManager.addIceCandidate(data);
                                }
                            }
                        } catch (Exception e) {
                        }
                    });
                    while (true) {
                        Thread.sleep(5000);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private static BufferedImage cropImageFromStr(String coordsStr, Dimension screenSize) {
        String[] parts = coordsStr.split(",");
        double xPct = Double.parseDouble(parts[0]);
        double yPct = Double.parseDouble(parts[1]);
        double wPct = Double.parseDouble(parts[2]);
        double hPct = Double.parseDouble(parts[3]);
        int x = (int) (xPct * screenSize.width);
        int y = (int) (yPct * screenSize.height);
        int w = (int) (wPct * screenSize.width);
        int h = (int) (hPct * screenSize.height);
        return remoteRobot.capture(x, y, w, h);
    }
}

