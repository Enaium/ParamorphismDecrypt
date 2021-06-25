package cn.enaium.paramorphism.decrypt;

import cn.enaium.paramorphism.decrypt.util.JFileChooserUtil;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

/**
 * @author Enaium
 */
public class ParamorphismDecrypt extends JFrame {
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
                        continue;
                    }
                    jarOutStream.putNextEntry(new ZipEntry(name));
                    jarOutStream.write(jarFile.getInputStream(jarEntry).readAllBytes());
                    jarOutStream.closeEntry();
                }
                jarFile.close();
                jarOutStream.setComment("Decrypt By Enaium");
                jarOutStream.close();
                JOptionPane.showMessageDialog(ParamorphismDecrypt.this, "Decrypt Success!", "Success", JOptionPane.INFORMATION_MESSAGE);
            } catch (IOException exception) {
                exception.printStackTrace();
                JOptionPane.showMessageDialog(ParamorphismDecrypt.this, exception.getMessage(), "ERROR", JOptionPane.ERROR_MESSAGE);
            }
        });
    }
}
