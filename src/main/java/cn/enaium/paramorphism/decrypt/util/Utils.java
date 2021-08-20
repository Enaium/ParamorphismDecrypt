package cn.enaium.paramorphism.decrypt.util;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.commons.ClassRemapper;
import org.objectweb.asm.commons.Remapper;
import org.objectweb.asm.tree.*;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

/**
 * @description: utils
 * @author: QianXia
 * @create: 2021/08/19 21:28
 **/
public class Utils {
    public static final Map<String, ClassNode> classes = new HashMap<>();
    public static final Map<String, ClassNode> addedClasses = new HashMap<>();
    public static final Map<String, byte[]> allThings = new HashMap<>();
    public static final Map<String, byte[]> otherThings = new HashMap<>();

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

    /**
     * Decrypt the class file that encrypted by the obfuscator
     *
     * @return decrypted times
     */
    public static int findAndDecrypt() {
        AtomicInteger number = new AtomicInteger();

        classes.forEach((name, classNode) -> classNode.methods.forEach(methodNode -> {
            if (!"<clinit>".equals(methodNode.name)) {
                return;
            }

            List<AbstractInsnNode> list = new ArrayList<>();
            List<AbstractInsnNode> toRemove = new ArrayList<>();
            AbstractInsnNode fieldInsnNode = null;
            for (AbstractInsnNode insn : methodNode.instructions) {
                // find Paramorphism decryption node
                if (insn instanceof MethodInsnNode &&
                        "()Ljava/lang/ClassLoader;".equals(((MethodInsnNode) insn).desc) &&
                        "getClassLoader".equals(((MethodInsnNode) insn).name) &&
                        insn.getNext() instanceof LdcInsnNode &&
                        insn.getNext().getNext() instanceof LdcInsnNode &&
                        insn.getNext().getNext().getNext().getOpcode() == Opcodes.INVOKESPECIAL &&
                        insn.getNext().getNext().getNext().getNext() instanceof LdcInsnNode &&
                        insn.getNext().getNext().getNext().getNext().getNext().getOpcode() == Opcodes.INVOKEVIRTUAL &&
                        insn.getNext().getNext().getNext().getNext().getNext().getNext().getOpcode() == Opcodes.INVOKEVIRTUAL) {

                    String encryptedClassName = (String) ((LdcInsnNode) insn.getNext()).cst;
                    int length = (int) ((LdcInsnNode) insn.getNext().getNext()).cst;

                    // decrypt the encrypted class
                    tryDecrypt(encryptedClassName, length);

                    // remove all useless code
                    fieldInsnNode = insn.getNext().getNext().getNext().getNext().getNext().getNext().getNext();
                    toRemove.addAll(removeBefore(fieldInsnNode));

                    // fix node
                    // 生成新的得到紫水晶生成的类的对象的指令
                    String simpleName = encryptedClassName.replace(".class", "");
                    list.add(new TypeInsnNode(Opcodes.NEW, simpleName));
                    list.add(new InsnNode(Opcodes.DUP));
                    list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, simpleName, "<init>", "()V"));

                    // remove the modified class from allThings to apply the changes
                    // 更改过的类需要移除，否则默认从allThings中取得byte不会应用更改
                    allThings.remove(classNode.name + ".class");

                    number.getAndIncrement();
                }
            }
            for (AbstractInsnNode node : toRemove) {
                methodNode.instructions.remove(node);
            }
            for (AbstractInsnNode node : list) {
                methodNode.instructions.insertBefore(fieldInsnNode, node);
            }
        }));

        classes.putAll(addedClasses);
        return number.get();
    }

    /**
     * remove nodes from the first node to @insn
     *
     * @param insn from first to here
     * @return all the nodes between the first node of the method and insn
     */
    private static List<AbstractInsnNode> removeBefore(AbstractInsnNode insn) {
        List<AbstractInsnNode> removeList = new ArrayList<>();
        AbstractInsnNode now = insn.getPrevious();
        while (now != null) {
            removeList.add(now);
            now = now.getPrevious();
        }

        return removeList;
    }

    /**
     * decrypt class from name and length
     * @param name encrypted class name
     * @param length read from obfuscated JAR file
     */
    private static void tryDecrypt(String name, int length) {
        byte[] encrypted = allThings.get(name);
        byte[] decrypted = null;
        try {
            InputStream var4 = new ByteArrayInputStream(encrypted);
            DataInputStream var5 = new DataInputStream(var4);
            decrypted = new byte[length];
            var5.readFully(decrypted, 0, var4.available());

            for(int var6 = 7; var6 >= 0; --var6) {
                decrypted[7 - var6] = (byte)((int)(2272919233031569408L >> 8 * var6 & 255L));
            }

            var4.close();
            GZIPInputStream var8 = new GZIPInputStream(new ByteArrayInputStream(decrypted.clone()));
            var5 = new DataInputStream(var8);
            var5.readFully(decrypted);
            var8.close();
        } catch (Exception ignored) {
        }

        // remove the encrypted class
        allThings.remove(name);
        readClass(name, decrypted, true);
        allThings.remove(name);
    }

    public static void readClass(String name, byte[] bytes) {
        readClass(name, bytes, false);
    }

    /**
     * avoid throwing ConcurrentModificationException
     * @param name class name
     * @param bytes class bytes
     * @param newMap avoid throwing ConcurrentModificationException
     */
    private static void readClass(String name, byte[] bytes, boolean newMap){
        try {
            ClassReader reader = new ClassReader(bytes);
            ClassNode node = new ClassNode();
            reader.accept(node, ClassReader.SKIP_FRAMES);
            if(newMap){
                node.name = name.replace(".class", "");
                addedClasses.put(node.name, node);
            } else {
                classes.put(node.name, node);
            }
        } catch (Exception ignored) {
        }
        // put all things to this map to get the bytes easily
        allThings.put(name, bytes);
    }

    public static void remap() {
        CustomRemapper remapper = new CustomRemapper();

        // package rename
        classes.forEach((name, classNode) -> {
            String packageName = name.lastIndexOf('/') == -1 ? "" : name.substring(0, name.lastIndexOf('/'));
            if (packageName.length() > 0) {
                int lin;
                while ((lin = packageName.lastIndexOf('/')) != -1) {
                    String parentPackage = packageName.substring(0, lin);
                    if (!remapper.mapPackage(packageName, removeJunkByte(packageName))) {
                        break;
                    }
                    packageName = parentPackage;
                }
                remapper.mapPackage(packageName, removeJunkByte(packageName));
            }
        });

        // class rename
        classes.forEach((name, clazz) -> remapper.map(name, removeJunkByte(name)));

        applyRemap(remapper);
    }

    private static void applyRemap(Remapper remapper) {
        Map<String, ClassNode> updated = new HashMap<>();
        Set<String> removed = new HashSet<>();

        classes.forEach((name, clazz) -> {
            removed.add(name);

            ClassNode newNode = new ClassNode();
            ClassRemapper classRemapper = new ClassRemapper(newNode, remapper);
            clazz.accept(classRemapper);

            updated.put(newNode.name, newNode);
        });

        removed.forEach(classes::remove);
        removed.forEach(name -> allThings.remove(name + ".class"));
        classes.putAll(updated);
    }

    public static void writeFile(JarOutputStream jarOutStream) throws IOException {
        for (String name : classes.keySet()) {
            // avoid Verify Error. or we need COMPUTE_FRAMES with libs
            byte[] b = allThings.get(name + ".class");

            if (b == null) {
                ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                ClassNode node = classes.get(name);
                node.accept(writer);
                b = writer.toByteArray();
            }

            /*
                一些JAR文件被混淆后，紫水晶会将类名或包名重命名为带有 字节：0x00 的名字的类（例如紫水晶本体）
                这样在压缩软件中是无法正常显示类名的
                将会显示为空名字的文件
                移除掉所有 0x00 之后才可以正常显示
                After the JAR file is obfuscated,
                Paramorphism will rename the class name or package name to random Java keyword with byte:0x00
                (for example, Paramorphism-1.3-Hotfix.jar)
                In this way, the class will be displayed as a file with an empty name in the decompression software
                It can be displayed normally after removing all 0x00
             */
            name = Utils.removeJunkByte(name + ".class");
            jarOutStream.putNextEntry(new ZipEntry(name));
            jarOutStream.write(b);
            jarOutStream.closeEntry();
        }

        // write resource or something
        for (String other : otherThings.keySet()) {
            jarOutStream.putNextEntry(new ZipEntry(other));
            jarOutStream.write(otherThings.get(other));
            jarOutStream.closeEntry();
        }

        jarOutStream.close();
    }
}
