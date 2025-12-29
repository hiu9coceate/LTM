package com.example.LTMang.services.media.capture;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.util.Iterator;
public class ScreenCaptureService {
    private Robot robot;
    private Rectangle screenRect;
    public ScreenCaptureService() { try { robot = new Robot(); screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize()); } catch(Exception e){} }
    public byte[] captureAsByteArray() {
        try {
            BufferedImage img = robot.createScreenCapture(screenRect);
            
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpg");
            ImageWriter writer = writers.next();
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(0.3f); 
            ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
            writer.setOutput(ios);
            writer.write(null, new javax.imageio.IIOImage(img, null, null), param);
            writer.dispose();
            return baos.toByteArray();
        } catch(Exception e) { return null; }
    }
}

