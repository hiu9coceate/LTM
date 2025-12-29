package com.example.LTMang.core;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.image.BufferedImage;
public class RemoteRobot {
    private Robot robot;
    private Dimension screenSize;
    public RemoteRobot() {
        try {
            this.robot = new Robot();
            this.screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        } catch (AWTException e) { e.printStackTrace(); }
    }
    public void moveMouse(float xRatio, float yRatio) {
        if (robot == null) return;
        int x = (int) (xRatio * screenSize.width);
        int y = (int) (yRatio * screenSize.height);
        robot.mouseMove(x, y);
    }
    public void click() {
        if (robot == null) return;
        robot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
        robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
    }
    public BufferedImage capture(int x, int y, int w, int h) {
        if (w <= 0) w = 1; if (h <= 0) h = 1;
        if (x < 0) x = 0; if (y < 0) y = 0;
        return robot.createScreenCapture(new Rectangle(x, y, w, h));
    }
    public Dimension getScreenSize() { return screenSize; }
}

