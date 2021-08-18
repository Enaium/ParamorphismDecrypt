package cn.enaium.paramorphism.decrypt;

import cn.enaium.paramorphism.decrypt.util.JFileChooserUtil;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;

/**
 * @author Enaium
 */
public class ParamorphismDecrypt extends JFrame {
    private static final Map<String, ClassNode> classes = new HashMap<>();
    private static final Map<String, ClassNode> addedClasses = new HashMap<>();
    private static final Map<String, byte[]> allThings = new HashMap<>();
    private static final Map<String, byte[]> otherThings = new HashMap<>();

    public static void main(String[] args) {
        new ParamorphismDecrypt().setVisible(true);
    }

    public ParamorphismDecrypt() {
        setTitle("Paramorphism Decrypt By Enaium");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(400, 150);
        setResizable(false);
        setLocationRelativeTo(null);
        setLayout(null);
        var jMenuBar = new JMenuBar();
        var jMenu = new JMenu("About");
        var gitHub = new JMenuItem("GitHub");
        gitHub.addActionListener(e -> {
            try {
                Desktop.getDesktop().browse(new URI("https://github.com/Enaium/ParamorphismDecrypt"));
            } catch (IOException | URISyntaxException exception) {
                exception.printStackTrace();
            }
        });
        jMenu.add(gitHub);
        jMenuBar.add(jMenu);
        setJMenuBar(jMenuBar);
        var inputLabel = new JLabel("Input Jar:");
        inputLabel.setBounds(5, 5, 70, 20);
        add(inputLabel);
        var inputTextField = new JTextField();
        inputTextField.setBounds(70, 5, 270, 20);
        add(inputTextField);
        var inputButton = new JButton("...");
        inputButton.setBounds(350, 5, 30, 20);
        add(inputButton);

        var outputLabel = new JLabel("Output Jar:");
        outputLabel.setBounds(5, 30, 70, 20);
        add(outputLabel);
        var outputTextField = new JTextField();
        outputTextField.setBounds(70, 30, 270, 20);
        add(outputTextField);
        var outputButton = new JButton("...");
        outputButton.setBounds(350, 30, 30, 20);
        add(outputButton);
        var decryptButton = new JButton("Decrypt");
        add(decryptButton);
        decryptButton.setBounds(5, 60, 375, 30);

        inputButton.addActionListener(e -> {
            var show = JFileChooserUtil.show(JFileChooserUtil.Type.OPEN);
            if (show != null) {
                inputTextField.setText(show.getPath());
                outputTextField.setText(show.getParent() + File.separator + show.getName().substring(0, show.getName().lastIndexOf(".")) + "-Decrypt" + show.getName().substring(show.getName().lastIndexOf(".")));
            }
        });

        outputButton.addActionListener(e -> {
            var show = JFileChooserUtil.show(JFileChooserUtil.Type.SAVE);
            if (show != null) {
                outputTextField.setText(show.getPath());
            }
        });

        decryptButton.addActionListener(e -> {
            try {
                var jarFile = new JarFile(inputTextField.getText());
                var jarOutStream = new JarOutputStream(new BufferedOutputStream(new FileOutputStream(outputTextField.getText())));
                var entries = jarFile.entries();
                while (entries.hasMoreElements()) {
                    var jarEntry = entries.nextElement();
                    String name;
                    if (jarEntry.getName().endsWith(".class")) {
                        name = jarEntry.getName();
                    } else if (jarEntry.getName().endsWith(".class/")) {
                        name = jarEntry.getName().substring(0, jarEntry.getName().length() - 1);
                    } else {
                        otherThings.put(jarEntry.getName(), jarFile.getInputStream(jarEntry).readAllBytes());
                        continue;
                    }
                    readClass(name, jarFile.getInputStream(jarEntry).readAllBytes());
                }

                int decrypted = findAndDecrypt();
                for (String name : classes.keySet()) {
                    byte[] b = allThings.get(name + ".class");

                    if (b == null) {
                        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
                        ClassNode node = classes.get(name);
                        node.accept(writer);
                        b = writer.toByteArray();
                    }

                    name = name + ".class";
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

                jarFile.close();
                jarOutStream.setComment("Decrypt By Enaium");
                jarOutStream.close();
                JOptionPane.showMessageDialog(ParamorphismDecrypt.this, "Decrypt Success!\nDecrypted " + decrypted + " classes!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(ParamorphismDecrypt.this, exception.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });
    }

    /**
     * Decrypt the class file that encrypted by the obfuscator
     */
    private int findAndDecrypt() {
        AtomicInteger number = new AtomicInteger();

        classes.forEach((name, classNode) -> {
            classNode.methods.forEach(methodNode -> {
                if (!"<clinit>".equals(methodNode.name)) {
                    return;
                }

                java.util.List<AbstractInsnNode> list = new ArrayList<>();
                java.util.List<AbstractInsnNode> toRemove = new ArrayList<>();
                AbstractInsnNode fieldInsnNode = null;
                for (AbstractInsnNode insn : methodNode.instructions) {
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

                        tryDecrypt(encryptedClassName, length);
                        // remove all useless code
                        fieldInsnNode = (insn.getNext().getNext().getNext().getNext().getNext().getNext().getNext());
                        toRemove.addAll(removeBefore(fieldInsnNode));

                        // 生成新的得到紫水晶生成的类的对象的指令
                        String simpleName = encryptedClassName.replace(".class", "");
                        list.add(new TypeInsnNode(Opcodes.NEW, simpleName));
                        list.add(new InsnNode(Opcodes.DUP));
                        list.add(new MethodInsnNode(Opcodes.INVOKESPECIAL, simpleName, "<init>", "()V"));

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
            });
        });

        classes.putAll(addedClasses);
        return number.get();
    }

    private java.util.List<AbstractInsnNode> removeBefore(AbstractInsnNode insn) {
        java.util.List<AbstractInsnNode> removeList = new ArrayList<>();
        AbstractInsnNode now = insn.getPrevious();
        while (now != null) {
            removeList.add(now);
            now = now.getPrevious();
        }

        return removeList;
    }

    private void tryDecrypt(String name, int length) {
        // cool Paramorphism
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
            GZIPInputStream var8 = new GZIPInputStream(new ByteArrayInputStream((byte[])decrypted.clone()));
            var5 = new DataInputStream(var8);
            var5.readFully(decrypted);
            var8.close();
        } catch (Exception ignored) {
        }

        // remove the encrypted class
        allThings.remove(name);
        readClass(name, decrypted, true);
        allThings.remove(name);        // (流汗黄豆)
    }

    private void readClass(String name, byte[] bytes) {
        readClass(name, bytes, false);
    }

    /**
     * avoid throwing the ConcurrentModificationException
     * @param name
     * @param bytes
     * @param newMap
     */
    private void readClass(String name, byte[] bytes, boolean newMap){
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
}
