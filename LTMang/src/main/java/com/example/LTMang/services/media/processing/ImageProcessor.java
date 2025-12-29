package com.example.LTMang.services.media.processing;
import com.example.LTMang.config.AppConfig;
import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.Iterator;
public class ImageProcessor {
    public static String compress(BufferedImage image) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName(AppConfig.IMG_FORMAT);
        ImageWriter writer = writers.next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        param.setCompressionQuality(AppConfig.IMG_QUALITY);
        ImageOutputStream ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);
        writer.write(null, new IIOImage(image, null, null), param);
        writer.dispose();
        return Base64.getEncoder().encodeToString(baos.toByteArray());
    }
}

