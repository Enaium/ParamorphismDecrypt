package cn.enaium.paramorphism.decrypt.util;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

/**
 * @description: byte
 * @author: QianXia
 * @create: 2021/08/19 21:28
 **/
public class ByteUtil {
    /**
     * Remove 0x00 in the string
     * [0, 1, 2, 3] -> [1, 2, 3]
     *
     * @param str in
     * @return cleaned string
     */
    public static String removeJunkByte(String str) {
        byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        for (byte b : bytes) {
            if (b != 0x00) {
                buffer.put(b);
            }
        }
        return new String(buffer.array());
    }
}
