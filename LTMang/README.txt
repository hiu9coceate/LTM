# NETVISION AI - CLEAN PROJECT
Đây là phiên bản sạch của dự án, đã loại bỏ các file rác và backup.

## Cấu trúc:
- src/main/java: Code xử lý chính (AppController, Services, Robot...)
- src/main/resources/static: Code Giao diện Web (HTML, CSS, JS)
- assets: Chứa Model OCR (tessdata) và Prompt AI (system_prompt.txt)
- pom.xml: Cấu hình thư viện Maven (Tess4J, JSON...)

## Cách chạy:
1. Mở thư mục này bằng VS Code / IntelliJ.
2. Đợi Maven tải thư viện (dựa vào pom.xml).
3. Chạy file: src/main/java/com/example/LTMang/app/ServerApp.java (Server Tín hiệu)
4. Chạy file: src/main/java/com/example/LTMang/main/LtMangLauncher.java (Client Java)
5. Mở Web: src/main/resources/static/index.html
