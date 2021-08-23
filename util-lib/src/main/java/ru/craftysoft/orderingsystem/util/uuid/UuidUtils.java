package ru.craftysoft.orderingsystem.util.uuid;

import java.security.SecureRandom;

public class UuidUtils {

    private static final SecureRandom secureRandom = new SecureRandom();

    private static final char[] digits = {
            '0', '1', '2', '3', '4', '5',
            '6', '7', '8', '9', 'a', 'b',
            'c', 'd', 'e', 'f', 'g', 'h',
            'i', 'j', 'k', 'l', 'm', 'n',
            'o', 'p', 'q', 'r', 's', 't',
            'u', 'v', 'w', 'x', 'y', 'z'
    };

    public static String generateDefaultUuid() {
        var randomBytes = new byte[16];
        secureRandom.nextBytes(randomBytes);
        long msb = 0;
        long lsb = 0;
        for (int i = 0; i < 8; i++) {
            msb = (msb << 8) | (randomBytes[i] & 0xff);
        }
        for (int i = 8; i < 16; i++) {
            lsb = (lsb << 8) | (randomBytes[i] & 0xff);
        }
        var buf = new byte[16];
        formatUnsignedLong(lsb >>> 48, buf, 12);
        formatUnsignedLong(lsb, buf, 8);
        formatUnsignedLong(msb >>> 16, buf, 4);
        formatUnsignedLong(msb >>> 32, buf, 0);
        return new String(buf);
    }

    private static void formatUnsignedLong(long val, byte[] buf, int offset) {
        int charPos = offset + 4;
        int radix = 1 << 4;
        int mask = radix - 1;
        do {
            buf[--charPos] = (byte) digits[((int) val) & mask];
            val >>>= 4;
        } while (charPos > offset);
    }

}
