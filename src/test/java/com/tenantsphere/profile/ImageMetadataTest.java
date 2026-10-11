package com.tenantsphere.profile;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.zip.CRC32;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import org.junit.jupiter.api.Test;

class ImageMetadataTest {

    private static final String PLACE = "GPS 51.5007 N 0.1246 W";
    // A 3 x 2 lossless WebP made with cwebp.
    private static final String WEBP_3X2 =
            "UklGRlAAAABXRUJQVlA4TEMAAAAvAkAAAC+gkI0kaBGW7oU9sCNR1LYRQ6HjsE2IB/Feitq2gQrhBFPMg7jX/Mf/J+iidIA824Gatg3YUOdfDo3ofzwrAA==";

    @Test
    void aJpegKeepsItsPixelsAndRotationButNotWhereItWasTaken() throws Exception {
        byte[] plain = encode(sample(BufferedImage.TYPE_INT_RGB), "jpeg");
        assertThat(applicationSegments(plain)).startsWith(0xE0);
        byte[] tagged = concat(
                Arrays.copyOfRange(plain, 0, 2),
                segment(0xE0, concat(ascii("JFIF"), new byte[] {0, 1, 2, 0, 0, 1, 0, 1, 5, 1}, ascii("THUMBNAIL-PIXEL"))),
                segment(0xE1, exif(true, 6)),
                segment(0xFE, ascii("Comment " + PLACE)),
                segment(0xED, ascii("Photoshop 3.0 " + PLACE)),
                Arrays.copyOfRange(plain, 4 + u16(plain, 4), plain.length));

        byte[] stripped = ImageMetadata.strip(tagged, "jpeg");

        assertThat(text(stripped)).doesNotContain(PLACE).doesNotContain("THUMBNAIL");
        assertThat(applicationSegments(stripped)).containsExactly(0xE0, 0xE1);
        assertThat(segmentData(stripped, 0xE1)).isEqualTo(new byte[] {
            'E', 'x', 'i', 'f', 0, 0, 'M', 'M', 0, 42, 0, 0, 0, 8, 0, 1,
            0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, 6, 0, 0, 0, 0, 0, 0});
        assertThat(pixels(stripped)).isEqualTo(pixels(plain));
    }

    @Test
    void anUprightJpegKeepsNoExifAtAll() throws Exception {
        byte[] plain = encode(sample(BufferedImage.TYPE_INT_RGB), "jpeg");
        byte[] tagged = concat(Arrays.copyOfRange(plain, 0, 2), segment(0xE1, exif(false, 1)),
                Arrays.copyOfRange(plain, 2, plain.length));

        byte[] stripped = ImageMetadata.strip(tagged, "jpeg");

        assertThat(text(stripped)).doesNotContain(PLACE);
        assertThat(applicationSegments(stripped)).containsExactly(0xE0);
        assertThat(pixels(stripped)).isEqualTo(pixels(plain));
    }

    @Test
    void aJpegLosesWhatIsStoredAfterItsEnd() throws Exception {
        byte[] plain = encode(sample(BufferedImage.TYPE_INT_RGB), "jpeg");
        // Some phones store a short video after the photo, with where it was taken.
        byte[] video = concat(new byte[] {0, 0, 0, 24}, ascii("ftypmp42"), ascii("udta xyz " + PLACE));

        byte[] stripped = ImageMetadata.strip(concat(plain, video), "jpeg");

        assertThat(stripped).isEqualTo(plain);
    }

    @Test
    void aProgressiveJpegKeepsEveryScanButNotACommentBetweenThem() throws Exception {
        byte[] plain = progressive(sample(BufferedImage.TYPE_INT_RGB));
        byte[] scan = {(byte) 0xFF, (byte) 0xDA};
        int second = indexOf(plain, scan, indexOf(plain, scan, 0) + 2);
        assertThat(second).isPositive();
        byte[] tagged = concat(Arrays.copyOf(plain, second), segment(0xFE, ascii("Comment " + PLACE)),
                Arrays.copyOfRange(plain, second, plain.length));

        byte[] stripped = ImageMetadata.strip(tagged, "jpeg");

        assertThat(text(stripped)).doesNotContain(PLACE);
        assertThat(stripped).isEqualTo(plain);
        assertThat(pixels(stripped)).isEqualTo(pixels(plain));
    }

    @Test
    void aPngKeepsItsPixelsButNotItsTextExifOrTime() throws Exception {
        byte[] plain = encode(sample(BufferedImage.TYPE_INT_ARGB), "png");
        int afterHeader = 8 + 12 + 13;
        byte[] tagged = concat(
                Arrays.copyOfRange(plain, 0, afterHeader),
                chunk("tEXt", concat(ascii("Location"), new byte[] {0}, ascii(PLACE))),
                chunk("eXIf", Arrays.copyOfRange(exif(false, 6), 6, exif(false, 6).length)),
                chunk("tIME", new byte[] {7, (byte) 0xEA, 10, 11, 12, 0, 0}),
                Arrays.copyOfRange(plain, afterHeader, plain.length));

        byte[] stripped = ImageMetadata.strip(tagged, "png");

        assertThat(text(stripped)).doesNotContain(PLACE);
        assertThat(pngChunks(stripped)).doesNotContain("tEXt", "eXIf", "tIME").contains("IHDR", "IDAT", "IEND");
        assertThat(pixels(stripped)).isEqualTo(pixels(plain));
    }

    @Test
    void aGifKeepsItsImageAndLoopingButNotItsComments() throws Exception {
        byte[] plain = encode(sample(BufferedImage.TYPE_INT_RGB), "gif");
        assertThat(plain[plain.length - 1]).isEqualTo((byte) 0x3B);
        byte[] tagged = concat(
                Arrays.copyOf(plain, plain.length - 1),
                gifExtension(0xFE, ascii("Comment " + PLACE)),
                gifApplication("XMP DataXMP", ascii("<x:xmpmeta>" + PLACE + "</x:xmpmeta>")),
                gifApplication("NETSCAPE2.0", new byte[] {1, 0, 0}),
                new byte[] {0x3B});

        byte[] stripped = ImageMetadata.strip(tagged, "gif");

        assertThat(text(stripped)).doesNotContain(PLACE).contains("NETSCAPE2.0");
        assertThat(pixels(stripped)).isEqualTo(pixels(plain));
    }

    @Test
    void aWebpKeepsItsImageButNotItsExifOrXmp() throws Exception {
        byte[] simple = Base64.getDecoder().decode(WEBP_3X2);
        byte[] extended = riff(
                webpChunk("VP8X", new byte[] {0x0C, 0, 0, 0, 2, 0, 0, 1, 0, 0}),
                Arrays.copyOfRange(simple, 12, simple.length),
                webpChunk("EXIF", Arrays.copyOfRange(exif(true, 6), 6, exif(true, 6).length)),
                webpChunk("XMP ", ascii("<x:xmpmeta>" + PLACE + "</x:xmpmeta>")));

        byte[] stripped = ImageMetadata.strip(extended, "webp");

        assertThat(text(stripped)).doesNotContain(PLACE);
        assertThat(webpChunks(stripped)).containsExactly("VP8X", "VP8L");
        assertThat(stripped[12 + 8] & 0x0C).isZero();
        assertThat(ByteBuffer.wrap(stripped, 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt()).isEqualTo(stripped.length - 8);
        assertThat(pixels(stripped)).isEqualTo(pixels(simple));
    }

    @Test
    void aFileNotLaidOutAsItsFormatSaysIsRefused() throws Exception {
        byte[] jpeg = encode(sample(BufferedImage.TYPE_INT_RGB), "jpeg");

        assertThatThrownBy(() -> ImageMetadata.strip(jpeg, "png")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ImageMetadata.strip(Arrays.copyOf(jpeg, 30), "jpeg"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static BufferedImage sample(int type) {
        BufferedImage image = new BufferedImage(6, 4, type);
        for (int x = 0; x < 6; x++) {
            for (int y = 0; y < 4; y++) image.setRGB(x, y, 0xFF000000 | (x * 40) << 16 | (y * 60) << 8 | 90);
        }
        return image;
    }

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        var out = new ByteArrayOutputStream();
        assertThat(ImageIO.write(image, format, out)).isTrue();
        return out.toByteArray();
    }

    // A JPEG drawn in several scans, each adding detail.
    private static byte[] progressive(BufferedImage image) throws Exception {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("jpeg").next();
        ImageWriteParam param = writer.getDefaultWriteParam();
        param.setProgressiveMode(ImageWriteParam.MODE_DEFAULT);
        var out = new ByteArrayOutputStream();
        try (var stream = ImageIO.createImageOutputStream(out)) {
            writer.setOutput(stream);
            writer.write(null, new IIOImage(image, null, null), param);
        } finally {
            writer.dispose();
        }
        return out.toByteArray();
    }

    private static int indexOf(byte[] in, byte[] part, int from) {
        for (int at = from; at + part.length <= in.length; at++) {
            if (Arrays.equals(in, at, at + part.length, part, 0, part.length)) return at;
        }
        return -1;
    }

    private static int[] pixels(byte[] file) throws Exception {
        BufferedImage image = ImageIO.read(new ByteArrayInputStream(file));
        return image.getRGB(0, 0, image.getWidth(), image.getHeight(), null, 0, image.getWidth());
    }

    // A TIFF directory with a description, as cameras write GPS and other text, and the orientation.
    private static byte[] exif(boolean little, int orientation) {
        byte[] description = concat(ascii(PLACE), new byte[] {0});
        ByteBuffer tiff = ByteBuffer.allocate(8 + 2 + 2 * 12 + 4 + description.length)
                .order(little ? ByteOrder.LITTLE_ENDIAN : ByteOrder.BIG_ENDIAN);
        tiff.put(ascii(little ? "II" : "MM")).putShort((short) 42).putInt(8).putShort((short) 2);
        tiff.putShort((short) 0x010E).putShort((short) 2).putInt(description.length).putInt(8 + 2 + 24 + 4);
        tiff.putShort((short) 0x0112).putShort((short) 3).putInt(1).putShort((short) orientation).putShort((short) 0);
        tiff.putInt(0).put(description);
        return concat(new byte[] {'E', 'x', 'i', 'f', 0, 0}, tiff.array());
    }

    private static byte[] segment(int marker, byte[] data) {
        return concat(new byte[] {(byte) 0xFF, (byte) marker, (byte) ((data.length + 2) >> 8), (byte) (data.length + 2)}, data);
    }

    private static List<Integer> applicationSegments(byte[] jpeg) {
        List<Integer> markers = new ArrayList<>();
        for (int i = 2; (jpeg[i + 1] & 0xFF) != 0xDA; i += 2 + u16(jpeg, i + 2)) {
            int marker = jpeg[i + 1] & 0xFF;
            if (marker >= 0xE0 && marker <= 0xEF || marker == 0xFE) markers.add(marker);
        }
        return markers;
    }

    private static byte[] segmentData(byte[] jpeg, int wanted) {
        for (int i = 2; (jpeg[i + 1] & 0xFF) != 0xDA; i += 2 + u16(jpeg, i + 2)) {
            if ((jpeg[i + 1] & 0xFF) == wanted) return Arrays.copyOfRange(jpeg, i + 4, i + 2 + u16(jpeg, i + 2));
        }
        throw new AssertionError("No segment " + Integer.toHexString(wanted));
    }

    private static byte[] chunk(String type, byte[] data) {
        CRC32 crc = new CRC32();
        crc.update(ascii(type));
        crc.update(data);
        return ByteBuffer.allocate(12 + data.length).putInt(data.length).put(ascii(type)).put(data)
                .putInt((int) crc.getValue()).array();
    }

    private static List<String> pngChunks(byte[] png) {
        List<String> types = new ArrayList<>();
        for (int i = 8; i < png.length; i += 12 + ByteBuffer.wrap(png, i, 4).getInt()) {
            types.add(new String(png, i + 4, 4, StandardCharsets.ISO_8859_1));
        }
        return types;
    }

    private static byte[] gifExtension(int label, byte[] data) {
        return concat(new byte[] {0x21, (byte) label}, subBlocks(data));
    }

    private static byte[] gifApplication(String identifier, byte[] data) {
        return concat(new byte[] {0x21, (byte) 0xFF, 11}, ascii(identifier), subBlocks(data));
    }

    private static byte[] subBlocks(byte[] data) {
        var out = new ByteArrayOutputStream();
        for (int i = 0; i < data.length; i += 255) {
            int size = Math.min(255, data.length - i);
            out.write(size);
            out.write(data, i, size);
        }
        out.write(0);
        return out.toByteArray();
    }

    private static byte[] webpChunk(String type, byte[] data) {
        ByteBuffer chunk = ByteBuffer.allocate(8 + data.length + (data.length & 1)).order(ByteOrder.LITTLE_ENDIAN);
        return chunk.put(ascii(type)).putInt(data.length).put(data).array();
    }

    private static byte[] riff(byte[]... chunks) {
        byte[] body = concat(chunks);
        ByteBuffer header = ByteBuffer.allocate(12).order(ByteOrder.LITTLE_ENDIAN);
        return concat(header.put(ascii("RIFF")).putInt(4 + body.length).put(ascii("WEBP")).array(), body);
    }

    private static List<String> webpChunks(byte[] webp) {
        List<String> types = new ArrayList<>();
        for (int i = 12; i + 8 <= webp.length; ) {
            int size = ByteBuffer.wrap(webp, i + 4, 4).order(ByteOrder.LITTLE_ENDIAN).getInt();
            types.add(new String(webp, i, 4, StandardCharsets.ISO_8859_1));
            i += 8 + size + (size & 1);
        }
        return types;
    }

    private static int u16(byte[] in, int at) {
        return (in[at] & 0xFF) << 8 | (in[at + 1] & 0xFF);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String text(byte[] file) {
        return new String(file, StandardCharsets.ISO_8859_1);
    }

    private static byte[] concat(byte[]... parts) {
        var out = new ByteArrayOutputStream();
        for (byte[] part : parts) out.writeBytes(part);
        return out.toByteArray();
    }
}
