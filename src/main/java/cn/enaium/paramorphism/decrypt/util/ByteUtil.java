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
     * https://blog.csdn.net/zhou452840622/article/details/104797262
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
        buffer.flip();
        byte[] validByte = new byte[buffer.limit()];
        buffer.get(validByte, 0, buffer.limit());
        return new String(validByte);
    }
}
