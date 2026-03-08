package encrypt;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;

public class GUI extends JFrame {

    private JRadioButton encryptRB = new JRadioButton("Cifrar");
    private JRadioButton decryptRB = new JRadioButton("Descifrar");

    private JButton keyButton = new JButton("Subir Llave");
    private JButton fileButton = new JButton("Subir Archivo");
    private JButton actionButton = new JButton("Ejecutar");

    private String keyPath;
    private String filePath;

    public GUI() {
        setTitle("Práctica 2");
        setSize(600, 400);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // --- PANEL NORTE (título y nombres) ---
        JPanel northPanel = new JPanel();
        northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.Y_AXIS));
        northPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10)); // márgenes

        JLabel title = new JLabel("Práctica 2: Función Criptográfica. Introduction to Cryptography");
        title.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel name1 = new JLabel("[]");
        name1.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel name2 = new JLabel("[]");
        name2.setAlignmentX(Component.CENTER_ALIGNMENT);
        JLabel name3 = new JLabel("[]");
        name3.setAlignmentX(Component.CENTER_ALIGNMENT);

        northPanel.add(title);
        northPanel.add(Box.createRigidArea(new Dimension(0, 5))); // espacio
        northPanel.add(name1);
        northPanel.add(name2);
        northPanel.add(name3);

        add(northPanel, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setBorder(BorderFactory.createEmptyBorder(10, 50, 10, 50)); // márgenes laterales

        JButton generate = new JButton("Generar Llaves");
        generate.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(generate);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
        ButtonGroup group = new ButtonGroup();
        group.add(encryptRB);
        group.add(decryptRB);
        radioPanel.add(encryptRB);
        radioPanel.add(decryptRB);
        centerPanel.add(radioPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        JPanel uploadPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        uploadPanel.add(keyButton);
        uploadPanel.add(fileButton);
        centerPanel.add(uploadPanel);
        centerPanel.add(Box.createRigidArea(new Dimension(0, 10)));

        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        centerPanel.add(actionButton);

        add(centerPanel, BorderLayout.CENTER);

        keyButton.setVisible(false);
        fileButton.setVisible(false);
        actionButton.setVisible(false);

        encryptRB.addActionListener(e -> toggleOptions());
        decryptRB.addActionListener(e -> toggleOptions());

        keyButton.addActionListener(this::selectKey);
        fileButton.addActionListener(this::selectFile);
        generate.addActionListener(e -> generateKeys());
        actionButton.addActionListener(e -> execute());

        setVisible(true);
    }

    private void toggleOptions() {
        keyButton.setVisible(true);
        fileButton.setVisible(true);
        actionButton.setVisible(true);

        if (encryptRB.isSelected()) {
            actionButton.setText("Cifrar");
        } else {
            actionButton.setText("Descifrar");
        }

        revalidate();
        repaint();
    }

    private void selectKey(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keyPath = chooser.getSelectedFile().getAbsolutePath();
        }
    }

    private void selectFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
        }
    }

    private void generateKeys() {
        // TODO Implementar generación de llaves
    }

    private void execute() {
        // TODO Implementar ejecución
    }
}