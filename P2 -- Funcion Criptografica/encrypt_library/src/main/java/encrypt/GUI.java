package encrypt;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.io.File;

public class GUI extends JFrame {

    // --- PALETA DE COLORES TIPO TAILWIND (Dark Mode) ---
    private final Color bgGeneral = new Color(15, 23, 42);   // Slate-900
    private final Color bgCard = new Color(30, 41, 59);      // Slate-800
    private final Color textMain = new Color(241, 245, 249); // Slate-100
    private final Color textSub = new Color(148, 163, 184);  // Slate-400
    private final Color primaryColor = new Color(59, 130, 246); // Blue-500
    private final Color primaryHover = new Color(37, 99, 235);  // Blue-600
    private final Color accentColor = new Color(16, 185, 129);  // Emerald-500
    private final Color accentHover = new Color(5, 150, 105);   // Emerald-600

    // Componentes que necesitan cambiar de tamaño dinámicamente
    private JLabel titulo;
    private JLabel subtitulo;
    private JButton generateBtn;
    private JRadioButton encryptRB = new JRadioButton("Cifrar Archivo");
    private JRadioButton decryptRB = new JRadioButton("Descifrar Archivo");
    private JButton keyButton = crearBotonModerno("📁 Subir Llave (.key)", primaryColor, primaryHover);
    private JButton fileButton = crearBotonModerno("📄 Subir Archivo", primaryColor, primaryHover);
    private JButton actionButton = crearBotonModerno("🚀 Ejecutar", accentColor, accentHover);
    private JLabel statusLabel = new JLabel("✨ Listo para empezar...");

    private String keyPath;
    private String filePath;

    public GUI() {
        setTitle("Criptografía RSA");
        setSize(750, 600); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        
        // El GridBagLayout actúa como un flexbox por asi decirlo para centrar el contendo 
        setLayout(new GridBagLayout());
        getContentPane().setBackground(bgGeneral);
        
        iniciarComponentes();
        configurarEscaladoResponsivo();
        
        setVisible(true);
    }

    private void iniciarComponentes() {
        // --- PANEL CARD PRINCIPAL ---
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(bgCard);
        cardPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(51, 65, 85), 1, true), 
            new EmptyBorder(40, 50, 40, 50)
        ));

        // 1. header
        titulo = new JLabel("Cifrado RSA Asimétrico");
        titulo.setForeground(textMain);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        subtitulo = new JLabel("Práctica 2 • Seguridad y Criptografía");
        subtitulo.setForeground(textSub);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titulo);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 10)));
        cardPanel.add(subtitulo);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 40)));

        // 2. generar laves 
        generateBtn = crearBotonModerno("🔑 Generar Nuevas Llaves RSA", new Color(71, 85, 105), new Color(51, 65, 85));
        generateBtn.setAlignmentX(Component.CENTER_ALIGNMENT);
        generateBtn.setMaximumSize(new Dimension(350, 50)); 
        generateBtn.addActionListener(e -> generateKeys());
        cardPanel.add(generateBtn);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        //  3. seleccionar modo     
        JPanel radioPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 40, 0));
        radioPanel.setBackground(bgCard);
        
        estilizarRadio(encryptRB);
        estilizarRadio(decryptRB);

        ButtonGroup group = new ButtonGroup();
        group.add(encryptRB);
        group.add(decryptRB);
        radioPanel.add(encryptRB);
        radioPanel.add(decryptRB);
        cardPanel.add(radioPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        // 4. Botones de los archivos
        JPanel uploadPanel = new JPanel(new GridLayout(1, 2, 25, 0));
        uploadPanel.setBackground(bgCard);
        uploadPanel.setMaximumSize(new Dimension(500, 55)); 
        
        uploadPanel.add(keyButton);
        uploadPanel.add(fileButton);
        cardPanel.add(uploadPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 35)));

        // 5. boton para ejecutar
        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionButton.setMaximumSize(new Dimension(300, 55)); 
        cardPanel.add(actionButton);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // 6. barra de status 
        statusLabel.setForeground(new Color(96, 165, 250)); 
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(statusLabel);

        // Agregamos la tarjeta completa al centro de la ventana
        add(cardPanel, new GridBagConstraints());
        
        encryptRB.addActionListener(e -> toggleOptions());
        decryptRB.addActionListener(e -> toggleOptions());
        keyButton.addActionListener(this::selectKey);
        fileButton.addActionListener(this::selectFile);
        actionButton.addActionListener(e -> execute());

        // Estado inicial oculto
        keyButton.setVisible(false);
        fileButton.setVisible(false);
        actionButton.setVisible(false);
    }

    private void configurarEscaladoResponsivo() {
        // Esto escucha cuando estiras o encojes la ventana
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                // Tomamos un ancho base de 750 para sacar la proporción
                float scale = getWidth() / 750f;
                // Limitamos la escala para que no se vea ridículo (min 80%, max 140%)
                scale = Math.max(0.8f, Math.min(scale, 1.4f));

                titulo.setFont(new Font("SansSerif", Font.BOLD, (int)(32 * scale)));
                subtitulo.setFont(new Font("SansSerif", Font.PLAIN, (int)(18 * scale)));
                generateBtn.setFont(new Font("SansSerif", Font.BOLD, (int)(15 * scale)));
                encryptRB.setFont(new Font("SansSerif", Font.PLAIN, (int)(18 * scale)));
                decryptRB.setFont(new Font("SansSerif", Font.PLAIN, (int)(18 * scale)));
                keyButton.setFont(new Font("SansSerif", Font.BOLD, (int)(15 * scale)));
                fileButton.setFont(new Font("SansSerif", Font.BOLD, (int)(15 * scale)));
                actionButton.setFont(new Font("SansSerif", Font.BOLD, (int)(18 * scale)));
                statusLabel.setFont(new Font("Monospaced", Font.BOLD, (int)(15 * scale)));
                revalidate();
            }
        });
    }

    private void estilizarRadio(JRadioButton rb) {
        rb.setBackground(bgCard);
        rb.setForeground(textMain);
        rb.setFocusPainted(false);
        rb.setCursor(new Cursor(Cursor.HAND_CURSOR));
    }

    private JButton crearBotonModerno(String texto, Color bg, Color hover) {
        JButton btn = new JButton(texto);
        btn.setForeground(Color.WHITE);
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));

        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent evt) {
                btn.setBackground(hover);
            }
            public void mouseExited(java.awt.event.MouseEvent evt) {
                btn.setBackground(bg);
            }
        });
        return btn;
    }

    private void toggleOptions() {
        keyButton.setVisible(true);
        fileButton.setVisible(true);
        actionButton.setVisible(true);
        statusLabel.setText("👉 Modo seleccionado. Por favor, suba los archivos.");

        if (encryptRB.isSelected()) {
            actionButton.setText("🔒 Ejecutar Cifrado");
        } else {
            actionButton.setText("🔓 Ejecutar Descifrado");
        }
        revalidate();
        repaint();
    }

    private void selectKey(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona tu archivo de Llave (.key)");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            keyPath = chooser.getSelectedFile().getAbsolutePath();
            statusLabel.setText("🔑 Llave cargada: " + chooser.getSelectedFile().getName());
        }
    }

    private void selectFile(ActionEvent e) {
        JFileChooser chooser = new JFileChooser();
        chooser.setDialogTitle("Selecciona el archivo a procesar");
        if (chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            filePath = chooser.getSelectedFile().getAbsolutePath();
            statusLabel.setText("📄 Archivo cargado: " + chooser.getSelectedFile().getName());
        }
    }

    private void generateKeys() {
        try {
            statusLabel.setText("⏳ Generando llaves, un momento...");
            Encrypt enc = new Encrypt();
            JFileChooser chooser = new JFileChooser();
            chooser.setDialogTitle("Guardar nueva llave RSA");
            chooser.setSelectedFile(new File("mi_llave_secreta.key"));
            
            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                String path = chooser.getSelectedFile().getAbsolutePath();
                if (!path.endsWith(".key")) path += ".key";
                enc.saveKeys(path);
                statusLabel.setText("✅ Llaves generadas con éxito.");
                JOptionPane.showMessageDialog(this, "Llaves guardadas correctamente en:\n" + path, "Éxito", JOptionPane.INFORMATION_MESSAGE);
            } else {
                statusLabel.setText("❌ Generación de llaves cancelada.");
            }
        } catch (Exception ex) {
            statusLabel.setText("❌ Error al generar llaves.");
            JOptionPane.showMessageDialog(this, "Error al generar llaves: " + ex.getMessage(), "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void execute() {
        if (!encryptRB.isSelected() && !decryptRB.isSelected()) {
            JOptionPane.showMessageDialog(this, "Selecciona Cifrar o Descifrar primero.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }
        if (keyPath == null || filePath == null) {
            JOptionPane.showMessageDialog(this, "Falta seleccionar la llave o el archivo.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            statusLabel.setText("⚙️ Procesando...");
            Encrypt enc = Encrypt.loadFromFile(keyPath);
            FileData input = new FileData(filePath);
            input.read();

            byte[] result;
            String actionName = encryptRB.isSelected() ? "cifrado" : "descifrado";
            
            if (encryptRB.isSelected()) {
                result = enc.encrypt(input.getData());
            } else {
                result = enc.decrypt(input.getData());
            }

            File inputFile = new File(filePath);
            String originalName = inputFile.getName();
            String outName;

            int dotIndex = originalName.lastIndexOf('.');
            if (dotIndex >= 0) {
                outName = originalName.substring(0, dotIndex) + "_" + actionName + originalName.substring(dotIndex);
            } else {
                outName = originalName + "_" + actionName;
            }

            JFileChooser chooser = new JFileChooser(inputFile.getParentFile());
            chooser.setDialogTitle("Guardar resultado del " + actionName);
            chooser.setSelectedFile(new File(inputFile.getParentFile(), outName));

            if (chooser.showSaveDialog(this) == JFileChooser.APPROVE_OPTION) {
                FileData output = new FileData(chooser.getSelectedFile().getAbsolutePath());
                output.write(result);
                statusLabel.setText("✅ ¡Proceso completado con éxito!");
                JOptionPane.showMessageDialog(this, "Archivo " + actionName + " guardado correctamente.", "Éxito", JOptionPane.INFORMATION_MESSAGE);
                
                // Limpiar selecciones
                keyPath = null;
                filePath = null;
            } else {
                statusLabel.setText("❌ Guardado cancelado.");
            }
        } catch (Exception ex) {
            statusLabel.setText("❌ Error en el proceso.");
            JOptionPane.showMessageDialog(this, "Error: " + ex.getMessage() + "\n¿Seguro que subiste la llave correcta?", "Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}