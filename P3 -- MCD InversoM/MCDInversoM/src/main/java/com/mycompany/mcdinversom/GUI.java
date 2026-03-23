package com.mycompany.mcdinversom;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;

/**
 * @author ricar
 * GUI Corregida - Procedimiento Completo (Euclides + Sustitución) xd
 */
public class GUI extends JFrame {

    // --- PALETA DE COLORES TAILWIND ---
    private final Color bgGeneral = new Color(15, 23, 42);   
    private final Color bgCard = new Color(30, 41, 59);      
    private final Color textMain = new Color(241, 245, 249); 
    private final Color textSub = new Color(148, 163, 184);  
    private final Color primaryColor = new Color(59, 130, 246); 
    private final Color primaryHover = new Color(37, 99, 235);  
    private final Color accentColor = new Color(52, 211, 153); 
    private final Color accentHover = new Color(16, 185, 129);  

    private JLabel titulo, subtitulo, statusLabel;
    private JTextField txtAlfa, txtBeta, txtN;
    private JTextArea outEstetico;
    private JButton actionButton, btnProcedimiento;
    private JScrollPane scrollOut;
    private String procedimientoCompleto = ""; 

    public GUI() {
        setTitle("Criptografía Afín"); 
        setSize(850, 900); 
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null); 
        setLayout(new GridBagLayout());
        getContentPane().setBackground(bgGeneral);
        
        iniciarComponentes();
        configurarEscaladoResponsivo();
        setVisible(true);
    }

    private void iniciarComponentes() {
        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(bgCard);
        cardPanel.setBorder(new CompoundBorder(
            new LineBorder(new Color(51, 65, 85), 1, true), 
            new EmptyBorder(30, 40, 30, 40)
        ));

        titulo = new JLabel("Cifrado (MCD Inverso)");
        titulo.setForeground(textMain);
        titulo.setAlignmentX(Component.CENTER_ALIGNMENT);
        
        subtitulo = new JLabel("Práctica Criptografía");
        subtitulo.setForeground(textSub);
        subtitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        cardPanel.add(titulo);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 30)));

        // Inputs
        JPanel inputPanel = new JPanel(new GridLayout(3, 2, 15, 10));
        inputPanel.setBackground(bgCard);
        inputPanel.setMaximumSize(new Dimension(500, 140));
        
        Font fuenteLabel = new Font("SansSerif", Font.BOLD, 18);
        txtAlfa = crearTextField();
        txtBeta = crearTextField();
        txtN = crearTextField();

        inputPanel.add(crearLabel("α (Alfa):", fuenteLabel)); inputPanel.add(txtAlfa);
        inputPanel.add(crearLabel("β (Beta):", fuenteLabel)); inputPanel.add(txtBeta);
        inputPanel.add(crearLabel("n (Módulo):", fuenteLabel)); inputPanel.add(txtN);
        
        cardPanel.add(inputPanel);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 30)));
        
        actionButton = crearBotonModerno("🚀 Calcular Funciones", accentColor, accentHover);
        actionButton.setAlignmentX(Component.CENTER_ALIGNMENT);
        actionButton.setMaximumSize(new Dimension(400, 55)); 
        cardPanel.add(actionButton);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 25)));

        outEstetico = new JTextArea();
        outEstetico.setBackground(new Color(15, 23, 42)); 
        outEstetico.setForeground(accentColor);
        outEstetico.setFont(new Font("Monospaced", Font.BOLD, 16));
        outEstetico.setEditable(false);
        outEstetico.setMargin(new Insets(10, 10, 10, 10));
        
        scrollOut = new JScrollPane(outEstetico);
        scrollOut.setMaximumSize(new Dimension(600, 250)); 
        scrollOut.setBorder(new LineBorder(new Color(71, 85, 105), 1));
        cardPanel.add(scrollOut);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 20)));

        btnProcedimiento = crearBotonModerno("📜 Mostrar Procedimiento", primaryColor, primaryHover);
        btnProcedimiento.setAlignmentX(Component.CENTER_ALIGNMENT);
        btnProcedimiento.setMaximumSize(new Dimension(400, 45)); 
        btnProcedimiento.setVisible(false);
        cardPanel.add(btnProcedimiento);
        cardPanel.add(Box.createRigidArea(new Dimension(0, 15)));

        statusLabel = new JLabel("✨ Listo para procesar...");
        statusLabel.setForeground(new Color(96, 165, 250)); 
        statusLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(statusLabel);

        add(cardPanel, new GridBagConstraints());
        
        actionButton.addActionListener(e -> executeAffine());
        btnProcedimiento.addActionListener(e -> showProcedureModal());
    }

    private JLabel crearLabel(String texto, Font fuente) {
        JLabel l = new JLabel(texto, SwingConstants.RIGHT);
        l.setForeground(textMain);
        l.setFont(fuente);
        return l;
    }

    private JTextField crearTextField() {
        JTextField f = new JTextField();
        f.setBackground(bgGeneral);
        f.setForeground(Color.WHITE);
        f.setCaretColor(Color.WHITE);
        f.setHorizontalAlignment(JTextField.CENTER);
        f.setFont(new Font("SansSerif", Font.BOLD, 20));
        f.setBorder(new LineBorder(new Color(71, 85, 105), 1));
        return f;
    }

    private JButton crearBotonModerno(String texto, Color bg, Color hover) {
        JButton btn = new JButton(texto);
        btn.setForeground(bgGeneral); 
        btn.setBackground(bg);
        btn.setFocusPainted(false);
        btn.setBorderPainted(false);
        btn.setFont(new Font("SansSerif", Font.BOLD, 16));
        btn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btn.addMouseListener(new java.awt.event.MouseAdapter() {
            public void mouseEntered(java.awt.event.MouseEvent e) { btn.setBackground(hover); }
            public void mouseExited(java.awt.event.MouseEvent e) { btn.setBackground(bg); }
        });
        return btn;
    }

    private void executeAffine() {
        try {
            int alfa = Integer.parseInt(txtAlfa.getText().trim());
            int beta = Integer.parseInt(txtBeta.getText().trim());
            int n = Integer.parseInt(txtN.getText().trim());

            MCDInversoM.RastroEuclides rastro = new MCDInversoM.RastroEuclides();
            
            // 1. Ejecutamos Euclides y guardamos esa parte del texto
            int mcdIndex = MCDInversoM.ejecutarEuclides(alfa, n, rastro);
            String logEuclides = rastro.log.toString(); 

            if (mcdIndex < 0 || rastro.residuos.get(mcdIndex) != 1) {
                outEstetico.setForeground(new Color(248, 113, 113));
                outEstetico.setText("❌ ERROR:\nMCD(" + alfa + ", " + n + ") != 1\nNo son coprimos.");
                btnProcedimiento.setVisible(false);
                return;
            }

            // 2. Ejecutamos Sustitución (limpiamos el log interno para capturar solo la 2da parte)
            rastro.log.setLength(0); 
            int inv = MCDInversoM.ejecutarSustitucion(alfa, n, rastro, mcdIndex);
            String logSustitucion = rastro.log.toString();

            // 3. FUSIONAMOS AMBAS PARTES PARA EL MODAL XD
            procedimientoCompleto = logEuclides + "\n\n" + logSustitucion;

            int tDesc = (inv * -beta) % n;
            if (tDesc < 0) tDesc += n;

            outEstetico.setForeground(accentColor);
            outEstetico.setText(
                "  INVERSO: α^-1 = " + inv + "\n\n" +
                "  CIFRADO:\n  C = (" + alfa + "P + " + beta + ") mod " + n + "\n\n" +
                "  DESCIFRADO:\n  P = " + inv + "(C + " + (n - beta) + ") mod " + n  + "\n\n" +
                "  P = (" + inv + "C + "+ inv*(n - beta) + ") mod " + n  
            );

            statusLabel.setText("✅ Cálculos realizados correctamente.");
            btnProcedimiento.setVisible(true);
            revalidate();

        } catch (Exception ex) {
            statusLabel.setText("❌ Error en los datos.");
        }
    }

    private void showProcedureModal() {
        JTextArea tx = new JTextArea(procedimientoCompleto);
        tx.setEditable(false);
        tx.setFont(new Font("Monospaced", Font.PLAIN, 15));
        tx.setBackground(bgGeneral);
        tx.setForeground(textMain);
        JScrollPane sp = new JScrollPane(tx);
        sp.setPreferredSize(new Dimension(700, 600)); // Un poco más ancho para las ecuaciones xd
        JOptionPane.showMessageDialog(this, sp, "Procedimiento Extendido", JOptionPane.PLAIN_MESSAGE);
    }

    private void configurarEscaladoResponsivo() {
        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
                float scale = getWidth() / 850f;
                titulo.setFont(new Font("SansSerif", Font.BOLD, (int)(32 * scale)));
                revalidate();
            }
        });
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new GUI());
    }
}