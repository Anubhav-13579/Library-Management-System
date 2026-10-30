import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;

/**
 * LoginFrame - Authentication Interface for Digital Library Management System
 * Matches the design and functionality in PBL Progress Report (Figure 2).
 */
public class LoginFrame extends JFrame {

    private JRadioButton adminRadio;
    private JRadioButton memberRadio;
    private JTextField usernameField;
    private JPasswordField passwordField;
    private JButton loginButton;
    private JButton clearButton;
    private CurrentUser currentUser;

    public LoginFrame() {
        setTitle("Digital Library Management System - Login");
        setSize(460, 560);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setResizable(false);
        setLayout(new BorderLayout());

        // Color Palette
        Color headerNavy = new Color(26, 54, 93);       // #1A365D
        Color bgLight = new Color(241, 245, 249);         // #F1F5F9
        Color primaryBlue = new Color(29, 78, 216);      // #1D4ED8
        Color textDark = new Color(30, 41, 59);          // #1E293B
        Color borderGray = new Color(203, 213, 225);      // #CBD5E1

        // ==========================================
        // 1. TOP HEADER (Deep Navy)
        // ==========================================
        JPanel topPanel = new JPanel();
        topPanel.setLayout(new BoxLayout(topPanel, BoxLayout.Y_AXIS));
        topPanel.setBackground(headerNavy);
        topPanel.setBorder(new EmptyBorder(18, 15, 18, 15));

        // Logo loader with fallback
        JLabel logoLabel = createLogoLabel();
        logoLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel titleLabel = new JLabel("Digital Library Management System");
        titleLabel.setForeground(Color.WHITE);
        titleLabel.setFont(new Font("Segoe UI", Font.BOLD, 17));
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel subtitleLabel = new JLabel("NIET Greater Noida");
        subtitleLabel.setForeground(new Color(203, 213, 225));
        subtitleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subtitleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        topPanel.add(logoLabel);
        topPanel.add(Box.createVerticalStrut(8));
        topPanel.add(titleLabel);
        topPanel.add(Box.createVerticalStrut(3));
        topPanel.add(subtitleLabel);

        add(topPanel, BorderLayout.NORTH);

        // ==========================================
        // 2. CENTER PANEL (White Card on Light Gray)
        // ==========================================
        JPanel backgroundPanel = new JPanel(new GridBagLayout());
        backgroundPanel.setBackground(bgLight);

        JPanel cardPanel = new JPanel();
        cardPanel.setLayout(new BoxLayout(cardPanel, BoxLayout.Y_AXIS));
        cardPanel.setBackground(Color.WHITE);
        cardPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1, true),
                new EmptyBorder(22, 28, 22, 28)
        ));
        cardPanel.setPreferredSize(new Dimension(380, 340));

        // Card Title
        JLabel cardTitle = new JLabel("User Login");
        cardTitle.setFont(new Font("Segoe UI", Font.BOLD, 18));
        cardTitle.setForeground(headerNavy);
        cardTitle.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(cardTitle);
        cardPanel.add(Box.createVerticalStrut(14));

        // Role Selection
        JPanel rolePanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 10, 0));
        rolePanel.setBackground(Color.WHITE);
        JLabel roleLabel = new JLabel("Login as:");
        roleLabel.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        roleLabel.setForeground(textDark);

        adminRadio = new JRadioButton("Admin", true);
        adminRadio.setBackground(Color.WHITE);
        adminRadio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        adminRadio.setFocusPainted(false);

        memberRadio = new JRadioButton("Member");
        memberRadio.setBackground(Color.WHITE);
        memberRadio.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        memberRadio.setFocusPainted(false);

        ButtonGroup roleGroup = new ButtonGroup();
        roleGroup.add(adminRadio);
        roleGroup.add(memberRadio);

        rolePanel.add(roleLabel);
        rolePanel.add(adminRadio);
        rolePanel.add(memberRadio);
        rolePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        cardPanel.add(rolePanel);
        cardPanel.add(Box.createVerticalStrut(14));

        // Username / ID Field
        JLabel userLabel = new JLabel("Username / ID");
        userLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        userLabel.setForeground(textDark);
        userLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(userLabel);
        cardPanel.add(Box.createVerticalStrut(4));

        usernameField = new JTextField();
        usernameField.setPreferredSize(new Dimension(320, 34));
        usernameField.setMaximumSize(new Dimension(320, 34));
        usernameField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        usernameField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        usernameField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(usernameField);
        cardPanel.add(Box.createVerticalStrut(12));

        // Password Field
        JLabel passLabel = new JLabel("Password");
        passLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        passLabel.setForeground(textDark);
        passLabel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(passLabel);
        cardPanel.add(Box.createVerticalStrut(4));

        passwordField = new JPasswordField();
        passwordField.setPreferredSize(new Dimension(320, 34));
        passwordField.setMaximumSize(new Dimension(320, 34));
        passwordField.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        passwordField.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1),
                new EmptyBorder(4, 8, 4, 8)
        ));
        passwordField.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(passwordField);
        cardPanel.add(Box.createVerticalStrut(20));

        // Action Buttons
        JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 12, 0));
        buttonPanel.setBackground(Color.WHITE);
        buttonPanel.setMaximumSize(new Dimension(320, 36));

        loginButton = new JButton("Login");
        loginButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        loginButton.setBackground(primaryBlue);
        loginButton.setForeground(Color.WHITE);
        loginButton.setFocusPainted(false);
        loginButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        clearButton = new JButton("Clear");
        clearButton.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        clearButton.setBackground(new Color(226, 232, 240));
        clearButton.setForeground(textDark);
        clearButton.setFocusPainted(false);
        clearButton.setCursor(new Cursor(Cursor.HAND_CURSOR));

        buttonPanel.add(loginButton);
        buttonPanel.add(clearButton);
        buttonPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        cardPanel.add(buttonPanel);

        backgroundPanel.add(cardPanel);
        add(backgroundPanel, BorderLayout.CENTER);

        // ==========================================
        // 3. BOTTOM FOOTER
        // ==========================================
        JLabel footerLabel = new JLabel("Java Swing · JDBC · MySQL 8.0", SwingConstants.CENTER);
        footerLabel.setFont(new Font("Segoe UI", Font.PLAIN, 11));
        footerLabel.setForeground(new Color(100, 116, 139));
        footerLabel.setBorder(new EmptyBorder(8, 0, 10, 0));
        footerLabel.setBackground(bgLight);
        footerLabel.setOpaque(true);
        add(footerLabel, BorderLayout.SOUTH);

        // ==========================================
        // 4. EVENT LISTENERS
        // ==========================================
        loginButton.addActionListener(e -> attemptLogin());
        clearButton.addActionListener(e -> clearFields());

        KeyAdapter enterSubmit = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    attemptLogin();
                }
            }
        };
        usernameField.addKeyListener(enterSubmit);
        passwordField.addKeyListener(enterSubmit);
    }

    private JLabel createLogoLabel() {
        String[] possiblePaths = {
                "src/resources/niet_logo.png",
                "resources/niet_logo.png",
                "niet_logo.png",
                "../../src/resources/niet_logo.png"
        };

        for (String path : possiblePaths) {
            File file = new File(path);
            if (file.exists()) {
                ImageIcon rawIcon = new ImageIcon(path);
                Image scaled = rawIcon.getImage().getScaledInstance(64, 64, Image.SCALE_SMOOTH);
                return new JLabel(new ImageIcon(scaled));
            }
        }

        return new JLabel("📖", SwingConstants.CENTER) {
            {
                setFont(new Font("Segoe UI Emoji", Font.PLAIN, 40));
            }
        };
    }

    private void clearFields() {
        usernameField.setText("");
        passwordField.setText("");
        adminRadio.setSelected(true);
        usernameField.requestFocus();
    }

    private void attemptLogin() {
        String id = usernameField.getText().trim();
        String password = new String(passwordField.getPassword());
        boolean isAdmin = adminRadio.isSelected();
        String role = isAdmin ? "Admin" : "Member";

        if (id.isEmpty() || password.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Please enter both Username/ID and Password.",
                    "Input Required",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        try (Connection conn = DBConnection.getConnection()) {
            if (isAdmin) {
                String query = "SELECT librarian_id, name FROM librarians WHERE librarian_id = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, id);
                    stmt.setString(2, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            currentUser = new CurrentUser(id, name, "Librarian");
                            JOptionPane.showMessageDialog(this,
                                    "Welcome back, " + name + "!\nRole: Librarian / Admin",
                                    "Login Successful",
                                    JOptionPane.INFORMATION_MESSAGE);
                            openDashboard("Librarian");
                            return;
                        }
                    }
                }
            } else {
                String query = "SELECT member_id, name FROM members WHERE member_id = ? AND password = ?";
                try (PreparedStatement stmt = conn.prepareStatement(query)) {
                    stmt.setString(1, id);
                    stmt.setString(2, password);
                    try (ResultSet rs = stmt.executeQuery()) {
                        if (rs.next()) {
                            String name = rs.getString("name");
                            currentUser = new CurrentUser(id, name, "Member");
                            JOptionPane.showMessageDialog(this,
                                    "Welcome back, " + name + "!\nRole: Library Member",
                                    "Login Successful",
                                    JOptionPane.INFORMATION_MESSAGE);
                            openDashboard("Member");
                            return;
                        }
                    }
                }
            }

            JOptionPane.showMessageDialog(this,
                    "Invalid " + role + " credentials.\nPlease check your ID and Password.",
                    "Authentication Failed",
                    JOptionPane.ERROR_MESSAGE);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this,
                    "Database connection error: " + ex.getMessage() +
                    "\n\nPlease ensure MySQL is running in XAMPP and 'lib_sys_db' is imported.",
                    "Connection Error",
                    JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openDashboard(String role) {
        if ("Librarian".equalsIgnoreCase(role) || "Admin".equalsIgnoreCase(role)) {
            new LibrarianDashboard().setVisible(true);
        } else {
            new MemberDashboard(currentUser).setVisible(true);
        }
        this.dispose();
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {}

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
