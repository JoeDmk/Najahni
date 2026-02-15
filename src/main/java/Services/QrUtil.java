package Services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.io.File;
import java.nio.file.Path;

public class QrUtil {

    public static File generateQrPng(String payload, String outputPath) throws Exception {
        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix matrix = writer.encode(payload, BarcodeFormat.QR_CODE, 300, 300);
        Path path = new File(outputPath).toPath();
        MatrixToImageWriter.writeToPath(matrix, "PNG", path);
        return path.toFile();
    }
}
