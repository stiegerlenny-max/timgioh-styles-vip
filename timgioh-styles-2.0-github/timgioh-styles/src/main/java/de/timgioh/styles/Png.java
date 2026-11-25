package de.timgioh.styles;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.util.zip.CRC32;
import java.util.zip.Deflater;
import java.util.zip.InflaterInputStream;

/** Minimaler PNG-Leser/-Schreiber (nur JDK, ohne AWT). Liest 8/16 Bit, alle Farbtypen, ohne Interlace. */
public final class Png {
    public final int width;
    public final int height;
    /** ARGB, Zeile fuer Zeile. */
    public final int[] argb;

    public Png(int width, int height, int[] argb) {
        this.width = width;
        this.height = height;
        this.argb = argb;
    }

    public Png copy() {
        return new Png(width, height, argb.clone());
    }

    /** @return null, wenn das Format nicht unterstuetzt wird. */
    public static Png decode(byte[] data) {
        try {
            return decode0(data);
        } catch (Exception | OutOfMemoryError e) {
            return null;
        }
    }

    private static Png decode0(byte[] data) throws IOException {
        DataInputStream in = new DataInputStream(new ByteArrayInputStream(data));
        byte[] sig = new byte[8];
        in.readFully(sig);
        if ((sig[0] & 0xFF) != 0x89 || sig[1] != 'P' || sig[2] != 'N' || sig[3] != 'G') {
            return null;
        }
        int w = 0, h = 0, depth = 0, type = 0, interlace = 0;
        byte[] palette = null;
        byte[] trns = null;
        ByteArrayOutputStream idat = new ByteArrayOutputStream();
        while (true) {
            int len;
            try {
                len = in.readInt();
            } catch (IOException e) {
                break;
            }
            byte[] tb = new byte[4];
            in.readFully(tb);
            String t = new String(tb, "ISO-8859-1");
            byte[] body = new byte[len];
            in.readFully(body);
            in.readInt(); // crc
            if (t.equals("IHDR")) {
                DataInputStream h2 = new DataInputStream(new ByteArrayInputStream(body));
                w = h2.readInt();
                h = h2.readInt();
                depth = h2.readUnsignedByte();
                type = h2.readUnsignedByte();
                h2.readUnsignedByte();
                h2.readUnsignedByte();
                interlace = h2.readUnsignedByte();
            } else if (t.equals("PLTE")) {
                palette = body;
            } else if (t.equals("tRNS")) {
                trns = body;
            } else if (t.equals("IDAT")) {
                idat.write(body);
            } else if (t.equals("IEND")) {
                break;
            }
        }
        if (interlace != 0 || w <= 0 || h <= 0 || w > 16384 || h > 16384) {
            return null;
        }
        int channels;
        switch (type) {
            case 0: channels = 1; break;
            case 2: channels = 3; break;
            case 3: channels = 1; break;
            case 4: channels = 2; break;
            case 6: channels = 4; break;
            default: return null;
        }
        if (depth != 1 && depth != 2 && depth != 4 && depth != 8 && depth != 16) {
            return null;
        }
        int bitsPerPixel = channels * depth;
        int bpp = Math.max(1, bitsPerPixel / 8);
        int stride = (w * bitsPerPixel + 7) / 8;
        byte[] raw = new InflaterInputStream(new ByteArrayInputStream(idat.toByteArray())).readAllBytes();
        if (raw.length < (stride + 1) * h) {
            return null;
        }
        byte[] cur = new byte[stride];
        byte[] prev = new byte[stride];
        int[] out = new int[w * h];
        int pos = 0;
        for (int y = 0; y < h; y++) {
            int ft = raw[pos++] & 0xFF;
            System.arraycopy(raw, pos, cur, 0, stride);
            pos += stride;
            for (int i = 0; i < stride; i++) {
                int a = i >= bpp ? cur[i - bpp] & 0xFF : 0;
                int b = prev[i] & 0xFF;
                int c = i >= bpp ? prev[i - bpp] & 0xFF : 0;
                int x = cur[i] & 0xFF;
                switch (ft) {
                    case 0: break;
                    case 1: x += a; break;
                    case 2: x += b; break;
                    case 3: x += (a + b) >> 1; break;
                    case 4: {
                        int p = a + b - c;
                        int pa = Math.abs(p - a), pb = Math.abs(p - b), pc = Math.abs(p - c);
                        x += (pa <= pb && pa <= pc) ? a : (pb <= pc ? b : c);
                        break;
                    }
                    default: return null;
                }
                cur[i] = (byte) x;
            }
            for (int x = 0; x < w; x++) {
                out[y * w + x] = pixel(cur, x, type, depth, palette, trns);
            }
            byte[] tmp = prev;
            prev = cur;
            cur = tmp;
        }
        return new Png(w, h, out);
    }

    private static int sample(byte[] row, int index, int depth) {
        if (depth == 8) {
            return row[index] & 0xFF;
        }
        if (depth == 16) {
            return row[index * 2] & 0xFF; // hohes Byte
        }
        int perByte = 8 / depth;
        int b = row[index / perByte] & 0xFF;
        int shift = 8 - depth * (index % perByte + 1);
        return (b >> shift) & ((1 << depth) - 1);
    }

    private static int pixel(byte[] row, int x, int type, int depth, byte[] pal, byte[] trns) {
        switch (type) {
            case 0: {
                int v = sample(row, x, depth);
                int full = depth >= 8 ? v : v * 255 / ((1 << depth) - 1);
                int a = 255;
                if (trns != null && trns.length >= 2) {
                    int tv = ((trns[0] & 0xFF) << 8) | (trns[1] & 0xFF);
                    int raw = depth == 16 ? v : v;
                    if (depth <= 8 && tv == raw) {
                        a = 0;
                    }
                }
                return a << 24 | full << 16 | full << 8 | full;
            }
            case 2: {
                int r = sample(row, x * 3, depth), g = sample(row, x * 3 + 1, depth), b = sample(row, x * 3 + 2, depth);
                int a = 255;
                if (trns != null && trns.length >= 6 && depth == 8) {
                    int tr = trns[1] & 0xFF, tg = trns[3] & 0xFF, tb = trns[5] & 0xFF;
                    if (tr == r && tg == g && tb == b) {
                        a = 0;
                    }
                }
                return a << 24 | r << 16 | g << 8 | b;
            }
            case 3: {
                int idx = sample(row, x, depth);
                if (pal == null || idx * 3 + 2 >= pal.length) {
                    return 0;
                }
                int a = (trns != null && idx < trns.length) ? trns[idx] & 0xFF : 255;
                return a << 24 | (pal[idx * 3] & 0xFF) << 16 | (pal[idx * 3 + 1] & 0xFF) << 8 | (pal[idx * 3 + 2] & 0xFF);
            }
            case 4: {
                int v = sample(row, x * 2, depth), a = sample(row, x * 2 + 1, depth);
                return a << 24 | v << 16 | v << 8 | v;
            }
            default: {
                int r = sample(row, x * 4, depth), g = sample(row, x * 4 + 1, depth);
                int b = sample(row, x * 4 + 2, depth), a = sample(row, x * 4 + 3, depth);
                return a << 24 | r << 16 | g << 8 | b;
            }
        }
    }

    public byte[] encode() {
        try {
            ByteArrayOutputStream raw = new ByteArrayOutputStream(height * (width * 4 + 1));
            for (int y = 0; y < height; y++) {
                raw.write(0);
                for (int x = 0; x < width; x++) {
                    int p = argb[y * width + x];
                    raw.write(p >> 16);
                    raw.write(p >> 8);
                    raw.write(p);
                    raw.write(p >>> 24);
                }
            }
            Deflater d = new Deflater(6);
            d.setInput(raw.toByteArray());
            d.finish();
            ByteArrayOutputStream z = new ByteArrayOutputStream();
            byte[] buf = new byte[8192];
            while (!d.finished()) {
                int n = d.deflate(buf);
                z.write(buf, 0, n);
            }
            d.end();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            out.write(new byte[] {(byte) 0x89, 'P', 'N', 'G', 13, 10, 26, 10});
            ByteArrayOutputStream ihdr = new ByteArrayOutputStream();
            writeInt(ihdr, width);
            writeInt(ihdr, height);
            ihdr.write(new byte[] {8, 6, 0, 0, 0});
            chunk(out, "IHDR", ihdr.toByteArray());
            chunk(out, "IDAT", z.toByteArray());
            chunk(out, "IEND", new byte[0]);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void writeInt(ByteArrayOutputStream o, int v) {
        o.write(v >>> 24);
        o.write(v >>> 16);
        o.write(v >>> 8);
        o.write(v);
    }

    private static void chunk(ByteArrayOutputStream out, String type, byte[] body) {
        writeInt(out, body.length);
        byte[] tb = type.getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
        out.write(tb, 0, 4);
        out.write(body, 0, body.length);
        CRC32 crc = new CRC32();
        crc.update(tb);
        crc.update(body);
        writeInt(out, (int) crc.getValue());
    }
}
