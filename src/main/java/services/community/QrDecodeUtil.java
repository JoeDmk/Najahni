package services.community;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

public class QrDecodeUtil {

    public static String decodeFromImage(File file) throws Exception {

        BufferedImage bufferedImage = ImageIO.read(file);
        if (bufferedImage == null) {
            throw new IllegalArgumentException("Invalid image file.");
        }

        BinaryBitmap bitmap = new BinaryBitmap(
                new HybridBinarizer(
                        new BufferedImageLuminanceSource(bufferedImage)
                )
        );

        Result result = new MultiFormatReader().decode(bitmap);

        return result.getText(); // returns payload string
    }
}
