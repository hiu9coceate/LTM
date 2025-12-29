package com.example.LTMang.config;

public class AppConfig {
    // URL Server Signaling (Thay bằng Ngrok nếu cần)
    public static final String SERVER_URL = "ws://localhost:8887";
    
    // Cấu hình ảnh
    public static final String IMG_FORMAT = "jpg";
    public static final float IMG_QUALITY = 0.5f;

    // Cấu hình AI
    public static final String GEMINI_API_KEY = "AIzaSyCf6vaFNfhedJ2VwJTco8tEGP09BNSxmgY"; 
    public static final String PROMPT_FILE_PATH = "assets/system_prompt.txt";
}
