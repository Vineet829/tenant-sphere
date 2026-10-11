package com.tenantsphere.profile;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Removes what an image file says about itself, such as where and when a photo was taken, the camera,
 * comments and preview thumbnails, and keeps only what draws the image. Nothing is decoded or encoded
 * again, so the image looks the same and animations still play. A photo's rotation is kept, so phone
 * photos still stand upright.
 */
final class ImageMetadata {

    private static final byte[] EXIF = {'E', 'x', 'i', 'f', 0, 0};
    private static final byte[] JFIF = {'J', 'F', 'I', 'F', 0};
    private static final byte[] ICC_PROFILE = {'I', 'C', 'C', '_', 'P', 'R', 'O', 'F', 'I', 'L', 'E', 0};
    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', '\r', '\n', 0x1A, '\n'};
    private static final int UPRIGHT = 1;
    private static final Set<String> PNG_DRAWING_CHUNKS = Set.of(
            "IHDR", "PLTE", "IDAT", "IEND", "tRNS", "cHRM", "gAMA", "iCCP", "sBIT", "sRGB", "cICP",
            "bKGD", "pHYs", "acTL", "fcTL", "fdAT");
    // Looping, animation and the colour profile.
    private static final Set<String> GIF_DRAWING_APPLICATIONS = Set.of("NETSCAPE2.0", "ANIMEXTS1.0", "ICCRGBG1012");

    private ImageMetadata() {}

    /** The image without its metadata. Throws IllegalArgumentException for a file not laid out as its format says. */
    static byte[] strip(byte[] image, String format) {
        try {
            return switch (format) {
                case "jpeg" -> jpeg(image);
                case "png" -> png(image);
                case "gif" -> gif(image);
                case "webp" -> webp(image);
                default -> throw new IllegalArgumentException("Unknown image format " + format);
            };
        } catch (IndexOutOfBoundsException exception) {
            throw new IllegalArgumentException("The " + format + " file ends too early", exception);
        }
    }

    // A JPEG is a run of marked segments up to the first scan, then its scans and the end marker.
    private static byte[] jpeg(byte[] in) {
        if (u8(in, 0) != 0xFF || u8(in, 1) != 0xD8) throw new IllegalArgumentException("Not a JPEG file");
        List<int[]> kept = new ArrayList<>();
        int orientation = UPRIGHT;
        int i = 2;
        while (true) {
            if (u8(in, i) != 0xFF) throw new IllegalArgumentException("Expected a JPEG marker");
            while (u8(in, i + 1) == 0xFF) i++;
            int marker = u8(in, i + 1);
            if (marker == 0xDA || marker == 0xD9) break;
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                kept.add(new int[] {i, i + 2});
                i += 2;
                continue;
            }
            int end = i + 2 + u16(in, i + 2, false);
            if (end < i + 4 || end > in.length) throw new IllegalArgumentException("A JPEG segment runs past the file");
            if (marker == 0xE1 && orientation == UPRIGHT && startsWith(in, i + 4, end, EXIF)) {
                orientation = orientation(in, i + 4 + EXIF.length, end);
            }
            if (drawsImage(in, marker, i + 4, end)) kept.add(new int[] {i, end});
            i = end;
        }

        var out = new ByteArrayOutputStream(in.length);
        out.write(in, 0, 2);
        boolean rotationWritten = orientation == UPRIGHT;
        for (int[] segment : kept) {
            if (u8(in, segment[0] + 1) == 0xE0) {
                // The JFIF header can carry a thumbnail of the photo; it is written again without one.
                out.write(new byte[] {(byte) 0xFF, (byte) 0xE0, 0, 16}, 0, 4);
                out.write(in, segment[0] + 4, 12);
                out.write(new byte[] {0, 0}, 0, 2);
                continue;
            }
            if (!rotationWritten) {
                out.writeBytes(rotation(orientation));
                rotationWritten = true;
            }
            out.write(in, segment[0], segment[1] - segment[0]);
        }
        if (!rotationWritten) out.writeBytes(rotation(orientation));
        scans(in, i, out);
        return out.toByteArray();
    }

    // From the first scan to the end of the image: each scan with its compressed data, the tables
    // between scans, and the end marker. Comments and application data between scans are left out, and
    // so is anything stored after the end, such as the video some phones attach to a photo, which can
    // say where it was taken. An image that stops without its end marker is kept as far as it goes.
    private static void scans(byte[] in, int i, ByteArrayOutputStream out) {
        while (i < in.length) {
            if (u8(in, i) != 0xFF) throw new IllegalArgumentException("Expected a JPEG marker");
            while (u8(in, i + 1) == 0xFF) i++;
            int marker = u8(in, i + 1);
            if (marker == 0xD9) {
                out.write(in, i, 2);
                return;
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD7)) {
                out.write(in, i, 2);
                i += 2;
                continue;
            }
            int end = i + 2 + u16(in, i + 2, false);
            if (end < i + 4 || end > in.length) throw new IllegalArgumentException("A JPEG segment runs past the file");
            if (drawsImage(in, marker, i + 4, end)) out.write(in, i, end - i);
            i = end;
            if (marker == 0xDA) {
                int data = i;
                i = afterCompressedData(in, i);
                out.write(in, data, i - data);
            }
        }
    }

    // A scan's compressed data runs up to the next marker other than a restart: within it, a 0xFF byte
    // is followed by 0x00, and a marker may be preceded by more 0xFF bytes.
    private static int afterCompressedData(byte[] in, int i) {
        while (i < in.length) {
            if (u8(in, i) != 0xFF || i + 1 == in.length) {
                i++;
                continue;
            }
            int next = u8(in, i + 1);
            if (next == 0x00 || (next >= 0xD0 && next <= 0xD7)) {
                i += 2;
            } else if (next == 0xFF) {
                i++;
            } else {
                return i;
            }
        }
        return in.length;
    }

    // Of the application segments and comments, only the JFIF header, the colour profile and
    // Adobe's colour transform change how the image is drawn.
    private static boolean drawsImage(byte[] in, int marker, int data, int end) {
        if (marker == 0xFE) return false;
        if (marker < 0xE0 || marker > 0xEF) return true;
        return switch (marker) {
            case 0xE0 -> end - data >= 14 && startsWith(in, data, end, JFIF);
            case 0xE2 -> startsWith(in, data, end, ICC_PROFILE);
            case 0xEE -> startsWith(in, data, end, ascii("Adobe"));
            default -> false;
        };
    }

    // Exif keeps its values in a TIFF directory, where tag 0x0112 is the orientation, 1 to 8.
    private static int orientation(byte[] in, int tiff, int end) {
        if (tiff + 8 > end) return UPRIGHT;
        boolean little;
        if (u8(in, tiff) == 'I' && u8(in, tiff + 1) == 'I') little = true;
        else if (u8(in, tiff) == 'M' && u8(in, tiff + 1) == 'M') little = false;
        else return UPRIGHT;
        long directory = tiff + u32(in, tiff + 4, little);
        if (directory + 2 > end) return UPRIGHT;
        int entries = u16(in, (int) directory, little);
        for (int k = 0; k < entries; k++) {
            long entry = directory + 2 + 12L * k;
            if (entry + 12 > end) return UPRIGHT;
            if (u16(in, (int) entry, little) == 0x0112 && u16(in, (int) entry + 2, little) == 3) {
                int value = u16(in, (int) entry + 8, little);
                return value >= 1 && value <= 8 ? value : UPRIGHT;
            }
        }
        return UPRIGHT;
    }

    // An Exif segment holding nothing but the orientation.
    private static byte[] rotation(int orientation) {
        return new byte[] {
            (byte) 0xFF, (byte) 0xE1, 0, 34,
            'E', 'x', 'i', 'f', 0, 0,
            'M', 'M', 0, 42, 0, 0, 0, 8,
            0, 1,
            0x01, 0x12, 0, 3, 0, 0, 0, 1, 0, (byte) orientation, 0, 0,
            0, 0, 0, 0
        };
    }

    // A PNG is a signature, then chunks of length, type, data and checksum.
    private static byte[] png(byte[] in) {
        if (!startsWith(in, 0, in.length, PNG_SIGNATURE)) throw new IllegalArgumentException("Not a PNG file");
        var out = new ByteArrayOutputStream(in.length);
        out.write(in, 0, PNG_SIGNATURE.length);
        int i = PNG_SIGNATURE.length;
        while (i < in.length) {
            long end = i + 12 + u32(in, i, false);
            if (end > in.length) throw new IllegalArgumentException("A PNG chunk runs past the file");
            String type = ascii(in, i + 4, 4);
            if (PNG_DRAWING_CHUNKS.contains(type)) out.write(in, i, (int) end - i);
            i = (int) end;
            if (type.equals("IEND")) break;
        }
        return out.toByteArray();
    }

    // A GIF is a header and colour table, then images and extensions, each ending in sub-blocks.
    private static byte[] gif(byte[] in) {
        if (!startsWith(in, 0, in.length, ascii("GIF87a")) && !startsWith(in, 0, in.length, ascii("GIF89a"))) {
            throw new IllegalArgumentException("Not a GIF file");
        }
        int i = 13 + colourTable(u8(in, 10));
        var out = new ByteArrayOutputStream(in.length);
        out.write(in, 0, i);
        while (i < in.length && u8(in, i) != 0x3B) {
            int start = i;
            boolean keep;
            if (u8(in, i) == 0x2C) {
                // An image: its descriptor, its own colours, the code size, then its data.
                i += 10 + colourTable(u8(in, i + 9)) + 1;
                keep = true;
            } else if (u8(in, i) == 0x21) {
                // An extension: frame timing and transparency, drawn text, or an application's data.
                int label = u8(in, i + 1);
                keep = label == 0xF9 || label == 0x01
                        || (label == 0xFF && u8(in, i + 2) == 11
                                && GIF_DRAWING_APPLICATIONS.contains(ascii(in, i + 3, 11)));
                i += 2;
            } else {
                throw new IllegalArgumentException("Unknown GIF block");
            }
            i = afterSubBlocks(in, i);
            if (keep) out.write(in, start, i - start);
        }
        out.write(0x3B);
        return out.toByteArray();
    }

    private static int colourTable(int packed) {
        return (packed & 0x80) == 0 ? 0 : 3 << ((packed & 7) + 1);
    }

    private static int afterSubBlocks(byte[] in, int i) {
        for (int size = u8(in, i); size != 0; size = u8(in, i)) i += 1 + size;
        return i + 1;
    }

    // A WebP is a RIFF file: "RIFF", its size, "WEBP", then chunks of type, size and data.
    private static byte[] webp(byte[] in) {
        if (!startsWith(in, 0, in.length, ascii("RIFF")) || !startsWith(in, 8, in.length, ascii("WEBP"))) {
            throw new IllegalArgumentException("Not a WebP file");
        }
        long riffEnd = Math.min(in.length, 8 + u32(in, 4, true));
        var out = new ByteArrayOutputStream(in.length);
        out.write(in, 0, 12);
        int extendedHeader = -1;
        int i = 12;
        while (i + 8 <= riffEnd) {
            String type = ascii(in, i, 4);
            long size = u32(in, i + 4, true);
            if (i + 8 + size > in.length) throw new IllegalArgumentException("A WebP chunk runs past the file");
            if (!type.equals("EXIF") && !type.equals("XMP ")) {
                if (type.equals("VP8X")) extendedHeader = out.size();
                out.write(in, i, 8 + (int) size);
                if ((size & 1) == 1) out.write(0);
            }
            i += 8 + (int) size + (int) (size & 1);
        }
        byte[] result = out.toByteArray();
        int riffSize = result.length - 8;
        for (int k = 0; k < 4; k++) result[4 + k] = (byte) (riffSize >>> (8 * k));
        // The extended header lists the chunks that follow; it no longer has Exif (0x08) or XMP (0x04).
        if (extendedHeader >= 0 && extendedHeader + 8 < result.length) result[extendedHeader + 8] &= (byte) ~0x0C;
        return result;
    }

    private static int u8(byte[] in, int at) {
        return in[at] & 0xFF;
    }

    private static int u16(byte[] in, int at, boolean little) {
        return little ? u8(in, at) | u8(in, at + 1) << 8 : u8(in, at) << 8 | u8(in, at + 1);
    }

    private static long u32(byte[] in, int at, boolean little) {
        return little
                ? u16(in, at, true) | (long) u16(in, at + 2, true) << 16
                : (long) u16(in, at, false) << 16 | u16(in, at + 2, false);
    }

    private static byte[] ascii(String text) {
        return text.getBytes(StandardCharsets.ISO_8859_1);
    }

    private static String ascii(byte[] in, int at, int length) {
        return new String(in, at, length, StandardCharsets.ISO_8859_1);
    }

    private static boolean startsWith(byte[] in, int at, int end, byte[] prefix) {
        if (at + prefix.length > Math.min(end, in.length)) return false;
        for (int k = 0; k < prefix.length; k++) {
            if (in[at + k] != prefix[k]) return false;
        }
        return true;
    }
}
