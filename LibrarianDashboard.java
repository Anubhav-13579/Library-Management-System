import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * LibrarianDashboard - Full-featured Admin & Librarian Management Console
 * Matches Screenshots (c) and (d) and Section 11 of the PBL Report.
 */
public class LibrarianDashboard extends JFrame {

    // Main Layout & Navigation
    private final CardLayout cardLayout = new CardLayout();
    private final JPanel contentArea = new JPanel(cardLayout);
    private final List<JButton> navButtons = new ArrayList<>();

    // Dashboard Overview widgets
    private JLabel totalBooksLabel;
    private JLabel totalMembersLabel;
    private JLabel issuedBooksLabel;
    private JLabel totalFineLabel;
    private DefaultTableModel recentTransactionsModel;

    // Books Module widgets
    private DefaultTableModel booksTableModel;
    private JTable booksTable;
    private TableRowSorter<DefaultTableModel> booksSorter;
    private JTextField bookSearchField;
    private JComboBox<String> bookCategoryFilter;

    // Issue Book Module widgets
    private JComboBox<String> issueMemberCombo;
    private JComboBox<String> issueBookCombo;
    private JTextField issueDateField;
    private JTextField dueDateField;
    private JLabel issueCalloutLabel;

    // Return Book Module widgets
    private JComboBox<String> returnTransactionCombo;
    private JLabel returnMemberVal;
    private JLabel returnBookVal;
    private JLabel returnIssueDateVal;
    private JLabel returnDueDateVal;
    private JLabel returnReturnDateVal;
    private JLabel returnDaysLateVal;
    private JLabel returnFineVal;
    private JButton returnSubmitBtn;
    private int currentReturnTransId = -1;
    private String currentReturnBookId = null;
    private String currentReturnMemberId = null;
    private int currentCalculatedFine = 0;

    // Members Module widgets
    private DefaultTableModel membersTableModel;
    private JTable membersTable;
    private TableRowSorter<DefaultTableModel> membersSorter;
    private JTextField memberSearchField;

    // Reports Module widgets
    private DefaultTableModel reportsTableModel;
    private JTable reportsTable;
    private JRadioButton reportIssuedRadio;
    private JRadioButton reportOverdueRadio;

    // Colors Palette
    private final Color sidebarBg = Color.WHITE;                    // Light sidebar for black text
    private final Color sidebarHover = new Color(243, 244, 246);  // #F3F4F6
    private final Color sidebarActive = new Color(219, 234, 254); // #DBEAFE
    private final Color primaryBlue = new Color(29, 78, 216);     // #1D4ED8
    private final Color successGreen = new Color(22, 163, 74);    // #16A34A
    private final Color dangerRed = new Color(220, 38, 38);       // #DC2626
    private final Color cardBg = Color.WHITE;
    private final Color contentBg = new Color(243, 244, 246);     // #F3F4F6
    private final Color borderGray = new Color(229, 231, 235);    // #E5E7EB
    private final Color textDark = new Color(17, 24, 39);         // #111827

    public LibrarianDashboard() {
        setTitle("Digital Library Management System - Admin Dashboard");
        setSize(1100, 700);
        setMinimumSize(new Dimension(980, 620));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Build Left Sidebar
        add(createSidebar(), BorderLayout.WEST);

        // 2. Build Content Area (Cards)
        contentArea.setBackground(contentBg);
        contentArea.add(createDashboardPanel(), "DASHBOARD");
        contentArea.add(createBooksPanel(), "BOOKS");
        contentArea.add(createIssueBookPanel(), "ISSUE");
        contentArea.add(createReturnBookPanel(), "RETURN");
        contentArea.add(createMembersPanel(), "MEMBERS");
        contentArea.add(createReportsPanel(), "REPORTS");

        add(contentArea, BorderLayout.CENTER);

        // Initial Data Load
        refreshDashboardStats();
        loadBooksData();
        loadMembersData();
        refreshIssueDropdowns();
        refreshReturnDropdown();
        loadReportsData();

        // Start on Dashboard
        switchPanel("DASHBOARD", 0);
    }

    // =========================================================================
    // SIDEBAR NAVIGATION
    // =========================================================================
    private JPanel createSidebar() {
        JPanel sidebar = new JPanel();
        sidebar.setPreferredSize(new Dimension(220, 0));
        sidebar.setBackground(sidebarBg);
        sidebar.setLayout(new BorderLayout());

        // Header
        JPanel headerPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 20));
        headerPanel.setBackground(sidebarBg);
        JLabel title = new JLabel("📖  Library System");
        title.setFont(new Font("Segoe UI", Font.BOLD, 17));
        title.setForeground(Color.BLACK);
        headerPanel.add(title);
        sidebar.add(headerPanel, BorderLayout.NORTH);

        // Nav Links
        JPanel navList = new JPanel();
        navList.setLayout(new BoxLayout(navList, BoxLayout.Y_AXIS));
        navList.setBackground(sidebarBg);
        navList.setBorder(new EmptyBorder(10, 10, 10, 10));

        String[][] items = {
                {"📊  Dashboard", "DASHBOARD"},
                {"📚  Books", "BOOKS"},
                {"📥  Issue Book", "ISSUE"},
                {"📤  Return Book", "RETURN"},
                {"👥  Members", "MEMBERS"},
                {"📑  Reports", "REPORTS"}
        };

        for (int i = 0; i < items.length; i++) {
            final int index = i;
            final String cardName = items[i][1];
            JButton btn = createNavButton(items[i][0]);
            btn.addActionListener(e -> switchPanel(cardName, index));
            navButtons.add(btn);
            navList.add(btn);
            navList.add(Box.createVerticalStrut(6));
        }

        sidebar.add(navList, BorderLayout.CENTER);

        // Footer / Logout
        JPanel footer = new JPanel(new BorderLayout());
        footer.setBackground(sidebarBg);
        footer.setBorder(new EmptyBorder(10, 10, 15, 10));

        JButton logoutBtn = createNavButton("🚪  Logout");
        logoutBtn.setBackground(new Color(153, 27, 27)); // Soft dark red
        logoutBtn.addActionListener(e -> {
            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to log out?",
                    "Confirm Logout", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                new LoginFrame().setVisible(true);
                this.dispose();
            }
        });
        footer.add(logoutBtn, BorderLayout.CENTER);
        sidebar.add(footer, BorderLayout.SOUTH);

        return sidebar;
    }
    private JButton createNavButton(String text) {
        JButton btn = new JButton(text);

        btn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        btn.setForeground(Color.BLACK);
        btn.setBackground(sidebarBg);

        btn.setOpaque(true);
        btn.setContentAreaFilled(true);
        btn.setBorderPainted(false);
        btn.setFocusPainted(false);

        btn.setHorizontalAlignment(SwingConstants.LEFT);
        btn.setBorder(new EmptyBorder(10, 15, 10, 15));
        btn.setMaximumSize(new Dimension(200, 42));

        return btn;
    }

    private void switchPanel(String cardName, int activeIndex) {
        cardLayout.show(contentArea, cardName);

        for (int i = 0; i < navButtons.size(); i++) {
            if (i == activeIndex) {
                navButtons.get(i).setBackground(sidebarActive);
                navButtons.get(i).setForeground(Color.BLACK);
                navButtons.get(i).setFont(new Font("Segoe UI", Font.BOLD, 14));
            } else {
                navButtons.get(i).setBackground(sidebarBg);
                navButtons.get(i).setForeground(Color.BLACK);
                navButtons.get(i).setFont(new Font("Segoe UI", Font.PLAIN, 14));
            }
        }

        if ("DASHBOARD".equals(cardName)) refreshDashboardStats();
        if ("BOOKS".equals(cardName)) loadBooksData();
        if ("ISSUE".equals(cardName)) refreshIssueDropdowns();
        if ("RETURN".equals(cardName)) refreshReturnDropdown();
        if ("MEMBERS".equals(cardName)) loadMembersData();
        if ("REPORTS".equals(cardName)) loadReportsData();
    }

    // =========================================================================
    // PANEL 1: DASHBOARD OVERVIEW
    // =========================================================================
    private JPanel createDashboardPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(contentBg);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel statsRow = new JPanel(new GridLayout(1, 4, 15, 0));
        statsRow.setBackground(contentBg);

        totalBooksLabel = new JLabel("0", SwingConstants.CENTER);
        totalMembersLabel = new JLabel("0", SwingConstants.CENTER);
        issuedBooksLabel = new JLabel("0", SwingConstants.CENTER);
        totalFineLabel = new JLabel("₹ 0", SwingConstants.CENTER);

        statsRow.add(createStatCard("Total Books", totalBooksLabel, new Color(37, 99, 235)));
        statsRow.add(createStatCard("Total Members", totalMembersLabel, new Color(16, 185, 129)));
        statsRow.add(createStatCard("Books Issued", issuedBooksLabel, new Color(245, 158, 11)));
        statsRow.add(createStatCard("Total Fines", totalFineLabel, new Color(239, 68, 68)));

        panel.add(statsRow, BorderLayout.NORTH);

        JPanel recentCard = new JPanel(new BorderLayout());
        recentCard.setBackground(Color.WHITE);
        recentCard.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel title = new JLabel("Recent Library Transactions");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(textDark);
        recentCard.add(title, BorderLayout.NORTH);

        recentTransactionsModel = new DefaultTableModel(
                new String[]{"Trans ID", "Member", "Book Title", "Issue Date", "Due Date", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable table = styleTable(new JTable(recentTransactionsModel));
        recentCard.add(new JScrollPane(table), BorderLayout.CENTER);

        panel.add(recentCard, BorderLayout.CENTER);
        return panel;
    }

    private JPanel createStatCard(String title, JLabel valueLabel, Color accentColor) {
        JPanel card = new JPanel(new BorderLayout());
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1, true),
                new EmptyBorder(15, 15, 15, 15)
        ));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        titleLbl.setForeground(new Color(107, 114, 128));

        valueLabel.setFont(new Font("Segoe UI", Font.BOLD, 26));
        valueLabel.setForeground(accentColor);

        card.add(titleLbl, BorderLayout.NORTH);
        card.add(valueLabel, BorderLayout.CENTER);
        return card;
    }

    private void refreshDashboardStats() {
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM books");
            if (rs.next()) totalBooksLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM members");
            if (rs.next()) totalMembersLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COUNT(*) FROM books WHERE is_issued = 1");
            if (rs.next()) issuedBooksLabel.setText(String.valueOf(rs.getInt(1)));

            rs = stmt.executeQuery("SELECT COALESCE(SUM(fine_amount), 0) FROM transactions");
            if (rs.next()) totalFineLabel.setText("₹ " + rs.getDouble(1));

            recentTransactionsModel.setRowCount(0);
            String recentQuery = "SELECT t.transaction_id, m.name, b.title, t.issue_date, t.due_date, t.return_date " +
                    "FROM transactions t " +
                    "JOIN members m ON t.member_id = m.member_id " +
                    "JOIN books b ON t.book_id = b.book_id " +
                    "ORDER BY t.transaction_id DESC LIMIT 10";
            rs = stmt.executeQuery(recentQuery);
            while (rs.next()) {
                Date retDate = rs.getDate("return_date");
                String status = (retDate != null) ? "Returned (" + retDate + ")" : "Issued";
                recentTransactionsModel.addRow(new Object[]{
                        "T-" + rs.getInt("transaction_id"),
                        rs.getString("name"),
                        rs.getString("title"),
                        rs.getDate("issue_date"),
                        rs.getDate("due_date"),
                        status
                });
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // PANEL 2: BOOK MANAGEMENT (Anubhav's Core Module)
    // =========================================================================
    private JPanel createBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(contentBg);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(contentBg);

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTools.setBackground(contentBg);

        JLabel searchLbl = new JLabel("Search Books:");
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        bookSearchField = new JTextField(16);
        bookSearchField.setPreferredSize(new Dimension(180, 32));

        bookCategoryFilter = new JComboBox<>(new String[]{"All Categories", "Computer Science", "Fiction", "Mathematics", "Science", "General"});
        bookCategoryFilter.setPreferredSize(new Dimension(140, 32));

        JButton searchBtn = new JButton("Filter");
        searchBtn.setBackground(primaryBlue);
        searchBtn.setForeground(Color.BLACK);
        searchBtn.setFocusPainted(false);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setFocusPainted(false);

        leftTools.add(searchLbl);
        leftTools.add(bookSearchField);
        leftTools.add(bookCategoryFilter);
        leftTools.add(searchBtn);
        leftTools.add(resetBtn);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTools.setBackground(contentBg);

        JButton addBookBtn = new JButton("+ Add New Book");
        addBookBtn.setBackground(successGreen);
        addBookBtn.setForeground(Color.BLACK);
        addBookBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addBookBtn.setFocusPainted(false);

        JButton deleteBookBtn = new JButton("Delete Book");
        deleteBookBtn.setBackground(dangerRed);
        deleteBookBtn.setForeground(Color.BLACK);
        deleteBookBtn.setFocusPainted(false);

        rightTools.add(addBookBtn);
        rightTools.add(deleteBookBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        booksTableModel = new DefaultTableModel(
                new String[]{"Book ID", "Title", "Author", "Category", "Status"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        booksTable = styleTable(new JTable(booksTableModel));
        booksSorter = new TableRowSorter<>(booksTableModel);
        booksTable.setRowSorter(booksSorter);

        panel.add(new JScrollPane(booksTable), BorderLayout.CENTER);

        Runnable applyFilter = () -> {
            String text = bookSearchField.getText().trim();
            String cat = (String) bookCategoryFilter.getSelectedItem();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            if (!text.isEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
            }
            if (cat != null && !cat.equals("All Categories")) {
                filters.add(RowFilter.regexFilter("(?i)^" + cat + "$", 3));
            }

            if (filters.isEmpty()) {
                booksSorter.setRowFilter(null);
            } else {
                booksSorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        searchBtn.addActionListener(e -> applyFilter.run());
        resetBtn.addActionListener(e -> {
            bookSearchField.setText("");
            bookCategoryFilter.setSelectedIndex(0);
            booksSorter.setRowFilter(null);
        });

        addBookBtn.addActionListener(e -> showAddBookDialog());

        deleteBookBtn.addActionListener(e -> {
            int selectedRow = booksTable.getSelectedRow();
            if (selectedRow == -1) {
                JOptionPane.showMessageDialog(this, "Please select a book to delete.");
                return;
            }
            int modelRow = booksTable.convertRowIndexToModel(selectedRow);
            String bookId = (String) booksTableModel.getValueAt(modelRow, 0);
            String titleVal = (String) booksTableModel.getValueAt(modelRow, 1);
            String status = (String) booksTableModel.getValueAt(modelRow, 4);

            if ("Issued".equalsIgnoreCase(status)) {
                JOptionPane.showMessageDialog(this,
                        "Cannot delete book '" + titleVal + "' while it is currently issued.",
                        "Action Denied", JOptionPane.WARNING_MESSAGE);
                return;
            }

            int confirm = JOptionPane.showConfirmDialog(this,
                    "Are you sure you want to delete book: " + titleVal + " (" + bookId + ")?",
                    "Confirm Deletion", JOptionPane.YES_NO_OPTION);

            if (confirm == JOptionPane.YES_OPTION) {
                deleteBook(bookId);
            }
        });

        return panel;
    }

    private void showAddBookDialog() {
        JTextField idField = new JTextField();
        JTextField titleField = new JTextField();
        JTextField authorField = new JTextField();
        JComboBox<String> categoryCombo = new JComboBox<>(
                new String[]{"Computer Science", "Fiction", "Mathematics", "Science", "General"});

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.add(new JLabel("Book ID (e.g. B004):"));
        form.add(idField);
        form.add(new JLabel("Title:"));
        form.add(titleField);
        form.add(new JLabel("Author:"));
        form.add(authorField);
        form.add(new JLabel("Category:"));
        form.add(categoryCombo);

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Book",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String title = titleField.getText().trim();
            String author = authorField.getText().trim();
            String category = (String) categoryCombo.getSelectedItem();

            if (id.isEmpty() || title.isEmpty() || author.isEmpty()) {
                JOptionPane.showMessageDialog(this, "All fields are required.");
                return;
            }

            String sql = "INSERT INTO books (book_id, title, author, category, is_issued) VALUES (?, ?, ?, ?, FALSE)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setString(2, title);
                stmt.setString(3, author);
                stmt.setString(4, category);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Book added successfully!");
                loadBooksData();
                refreshIssueDropdowns();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding book: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void deleteBook(String bookId) {
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, bookId);
            stmt.executeUpdate();
            JOptionPane.showMessageDialog(this, "Book deleted successfully.");
            loadBooksData();
            refreshIssueDropdowns();
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Error deleting book: " + ex.getMessage(),
                    "Database Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadBooksData() {
        booksTableModel.setRowCount(0);
        String sql = "SELECT book_id, title, author, category, is_issued FROM books ORDER BY book_id ASC";
        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                boolean isIssued = rs.getBoolean("is_issued");
                booksTableModel.addRow(new Object[]{
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        isIssued ? "Issued" : "Available"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // PANEL 3: ISSUE BOOK MODULE (Matching Screenshot c)
    // =========================================================================
    private JPanel createIssueBookPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(contentBg);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));
        card.setPreferredSize(new Dimension(480, 480));

        JLabel title = new JLabel("📥  Issue Book");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(18));

        issueMemberCombo = new JComboBox<>();
        issueMemberCombo.setPreferredSize(new Dimension(420, 36));
        issueMemberCombo.setMaximumSize(new Dimension(420, 36));

        issueBookCombo = new JComboBox<>();
        issueBookCombo.setPreferredSize(new Dimension(420, 36));
        issueBookCombo.setMaximumSize(new Dimension(420, 36));

        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(14);

        issueDateField = new JTextField(today.toString());
        issueDateField.setEditable(false);
        issueDateField.setPreferredSize(new Dimension(420, 34));
        issueDateField.setMaximumSize(new Dimension(420, 34));

        dueDateField = new JTextField(due.toString());
        dueDateField.setEditable(false);
        dueDateField.setPreferredSize(new Dimension(420, 34));
        dueDateField.setMaximumSize(new Dimension(420, 34));

        addFormField(card, "Select Member", issueMemberCombo);
        addFormField(card, "Select Book (Available only)", issueBookCombo);
        addFormField(card, "Issue Date", issueDateField);
        addFormField(card, "Due Date (14 Days Standard)", dueDateField);

        JPanel callout = new JPanel(new FlowLayout(FlowLayout.CENTER));
        callout.setBackground(new Color(239, 246, 255));
        callout.setBorder(new LineBorder(new Color(191, 219, 254), 1));
        callout.setMaximumSize(new Dimension(420, 40));
        issueCalloutLabel = new JLabel("Due Date: " + due + "  |  Fine (if delayed): ₹2 per day");
        issueCalloutLabel.setForeground(new Color(30, 64, 175));
        issueCalloutLabel.setFont(new Font("Segoe UI", Font.BOLD, 12));
        callout.add(issueCalloutLabel);
        card.add(callout);
        card.add(Box.createVerticalStrut(20));

        JButton issueBtn = new JButton("Issue Book");
        issueBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        issueBtn.setBackground(primaryBlue);
        issueBtn.setForeground(Color.BLACK);
        issueBtn.setMaximumSize(new Dimension(420, 38));
        issueBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        issueBtn.setFocusPainted(false);
        issueBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.add(issueBtn);

        issueBtn.addActionListener(e -> executeIssueBook());

        wrapper.add(card);
        return wrapper;
    }

    private void addFormField(JPanel container, String labelText, JComponent field) {
        JLabel label = new JLabel(labelText);
        label.setFont(new Font("Segoe UI", Font.BOLD, 12));
        label.setForeground(new Color(75, 85, 99));
        label.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(label);
        container.add(Box.createVerticalStrut(4));
        field.setAlignmentX(Component.LEFT_ALIGNMENT);
        container.add(field);
        container.add(Box.createVerticalStrut(12));
    }

    private void refreshIssueDropdowns() {
        issueMemberCombo.removeAllItems();
        issueBookCombo.removeAllItems();

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement()) {

            ResultSet rs = stmt.executeQuery("SELECT member_id, name FROM members ORDER BY name ASC");
            while (rs.next()) {
                issueMemberCombo.addItem(rs.getString("name") + " (" + rs.getString("member_id") + ")");
            }

            rs = stmt.executeQuery("SELECT book_id, title FROM books WHERE is_issued = 0 ORDER BY title ASC");
            while (rs.next()) {
                issueBookCombo.addItem(rs.getString("title") + " (" + rs.getString("book_id") + ")");
            }

        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void executeIssueBook() {
        if (issueMemberCombo.getSelectedItem() == null || issueBookCombo.getSelectedItem() == null) {
            JOptionPane.showMessageDialog(this, "Please select both a Member and an Available Book.");
            return;
        }

        String memberItem = (String) issueMemberCombo.getSelectedItem();
        String bookItem = (String) issueBookCombo.getSelectedItem();

        String memberId = memberItem.substring(memberItem.lastIndexOf('(') + 1, memberItem.lastIndexOf(')'));
        String bookId = bookItem.substring(bookItem.lastIndexOf('(') + 1, bookItem.lastIndexOf(')'));

        LocalDate today = LocalDate.now();
        LocalDate due = today.plusDays(14);

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String checkSql = "SELECT is_issued FROM books WHERE book_id = ?";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, bookId);
                ResultSet rs = checkStmt.executeQuery();
                if (!rs.next() || rs.getBoolean("is_issued")) {
                    JOptionPane.showMessageDialog(this, "Selected book is no longer available.");
                    conn.rollback();
                    return;
                }
            }

            String transSql = "INSERT INTO transactions (book_id, member_id, issue_date, due_date, return_date, fine_amount) " +
                    "VALUES (?, ?, ?, ?, NULL, 0)";
            try (PreparedStatement stmt = conn.prepareStatement(transSql)) {
                stmt.setString(1, bookId);
                stmt.setString(2, memberId);
                stmt.setDate(3, Date.valueOf(today));
                stmt.setDate(4, Date.valueOf(due));
                stmt.executeUpdate();
            }

            String bookSql = "UPDATE books SET is_issued = 1 WHERE book_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(bookSql)) {
                stmt.setString(1, bookId);
                stmt.executeUpdate();
            }

            conn.commit();
            JOptionPane.showMessageDialog(this, "Book successfully issued!\nDue Date: " + due,
                    "Success", JOptionPane.INFORMATION_MESSAGE);

            refreshIssueDropdowns();
            refreshReturnDropdown();
            loadBooksData();
            refreshDashboardStats();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error issuing book: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PANEL 4: RETURN BOOK MODULE (Matching Screenshot c)
    // =========================================================================
    private JPanel createReturnBookPanel() {
        JPanel wrapper = new JPanel(new GridBagLayout());
        wrapper.setBackground(contentBg);

        JPanel card = new JPanel();
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1, true),
                new EmptyBorder(25, 30, 25, 30)
        ));
        card.setPreferredSize(new Dimension(500, 520));

        JLabel title = new JLabel("📤  Return Book");
        title.setFont(new Font("Segoe UI", Font.BOLD, 18));
        title.setForeground(textDark);
        title.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(title);
        card.add(Box.createVerticalStrut(16));

        JLabel selectLbl = new JLabel("Active Borrowed Transactions:");
        selectLbl.setFont(new Font("Segoe UI", Font.BOLD, 12));
        selectLbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(selectLbl);
        card.add(Box.createVerticalStrut(4));

        returnTransactionCombo = new JComboBox<>();
        returnTransactionCombo.setMaximumSize(new Dimension(440, 36));
        returnTransactionCombo.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(returnTransactionCombo);
        card.add(Box.createVerticalStrut(14));

        JPanel detailsPanel = new JPanel(new GridLayout(7, 2, 8, 8));
        detailsPanel.setBackground(new Color(249, 250, 251));
        detailsPanel.setBorder(BorderFactory.createCompoundBorder(
                new LineBorder(borderGray, 1),
                new EmptyBorder(12, 12, 12, 12)
        ));
        detailsPanel.setMaximumSize(new Dimension(440, 230));

        returnMemberVal = new JLabel("-");
        returnBookVal = new JLabel("-");
        returnIssueDateVal = new JLabel("-");
        returnDueDateVal = new JLabel("-");
        returnReturnDateVal = new JLabel("-");
        returnDaysLateVal = new JLabel("-");
        returnFineVal = new JLabel("-");
        returnFineVal.setForeground(dangerRed);
        returnFineVal.setFont(new Font("Segoe UI", Font.BOLD, 13));

        detailsPanel.add(new JLabel("Member Name:"));
        detailsPanel.add(returnMemberVal);
        detailsPanel.add(new JLabel("Book Title:"));
        detailsPanel.add(returnBookVal);
        detailsPanel.add(new JLabel("Issue Date:"));
        detailsPanel.add(returnIssueDateVal);
        detailsPanel.add(new JLabel("Due Date:"));
        detailsPanel.add(returnDueDateVal);
        detailsPanel.add(new JLabel("Return Date:"));
        detailsPanel.add(returnReturnDateVal);
        detailsPanel.add(new JLabel("Days Late:"));
        detailsPanel.add(returnDaysLateVal);
        detailsPanel.add(new JLabel("Fine Amount:"));
        detailsPanel.add(returnFineVal);

        detailsPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        card.add(detailsPanel);
        card.add(Box.createVerticalStrut(18));

        returnSubmitBtn = new JButton("Confirm Return & Pay Fine");
        returnSubmitBtn.setFont(new Font("Segoe UI", Font.BOLD, 14));
        returnSubmitBtn.setBackground(successGreen);
        returnSubmitBtn.setForeground(Color.BLACK);
        returnSubmitBtn.setMaximumSize(new Dimension(440, 38));
        returnSubmitBtn.setAlignmentX(Component.LEFT_ALIGNMENT);
        returnSubmitBtn.setFocusPainted(false);
        returnSubmitBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.add(returnSubmitBtn);

        returnTransactionCombo.addActionListener(e -> updateReturnDetails());
        returnSubmitBtn.addActionListener(e -> executeReturnBook());

        wrapper.add(card);
        return wrapper;
    }

    private void refreshReturnDropdown() {
        returnTransactionCombo.removeAllItems();
        String sql = "SELECT t.transaction_id, m.name, b.title " +
                "FROM transactions t " +
                "JOIN members m ON t.member_id = m.member_id " +
                "JOIN books b ON t.book_id = b.book_id " +
                "WHERE t.return_date IS NULL ORDER BY t.transaction_id DESC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                returnTransactionCombo.addItem("T-" + rs.getInt("transaction_id") + " : " +
                        rs.getString("title") + " (" + rs.getString("name") + ")");
            }
            updateReturnDetails();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void updateReturnDetails() {
        String selected = (String) returnTransactionCombo.getSelectedItem();
        if (selected == null) {
            returnMemberVal.setText("-");
            returnBookVal.setText("-");
            returnIssueDateVal.setText("-");
            returnDueDateVal.setText("-");
            returnReturnDateVal.setText("-");
            returnDaysLateVal.setText("-");
            returnFineVal.setText("-");
            currentReturnTransId = -1;
            returnSubmitBtn.setEnabled(false);
            return;
        }

        returnSubmitBtn.setEnabled(true);
        int transId = Integer.parseInt(selected.substring(2, selected.indexOf(':')).trim());
        currentReturnTransId = transId;

        String sql = "SELECT t.transaction_id, t.book_id, t.member_id, m.name AS member_name, " +
                "b.title AS book_title, t.issue_date, t.due_date " +
                "FROM transactions t " +
                "JOIN members m ON t.member_id = m.member_id " +
                "JOIN books b ON t.book_id = b.book_id " +
                "WHERE t.transaction_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, transId);
            ResultSet rs = stmt.executeQuery();

            if (rs.next()) {
                currentReturnBookId = rs.getString("book_id");
                currentReturnMemberId = rs.getString("member_id");
                LocalDate dueDate = rs.getDate("due_date").toLocalDate();
                LocalDate today = LocalDate.now();

                long daysLate = ChronoUnit.DAYS.between(dueDate, today);
                if (daysLate < 0) daysLate = 0;
                currentCalculatedFine = (int) (daysLate * 2);

                returnMemberVal.setText(rs.getString("member_name") + " (" + currentReturnMemberId + ")");
                returnBookVal.setText(rs.getString("book_title"));
                returnIssueDateVal.setText(rs.getDate("issue_date").toString());
                returnDueDateVal.setText(dueDate.toString());
                returnReturnDateVal.setText(today.toString());
                returnDaysLateVal.setText(daysLate + " day(s)");
                returnFineVal.setText("₹ " + currentCalculatedFine);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private void executeReturnBook() {
        if (currentReturnTransId <= 0) return;

        LocalDate today = LocalDate.now();

        try (Connection conn = DBConnection.getConnection()) {
            conn.setAutoCommit(false);

            String transSql = "UPDATE transactions SET return_date = ?, fine_amount = ? WHERE transaction_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(transSql)) {
                stmt.setDate(1, Date.valueOf(today));
                stmt.setDouble(2, currentCalculatedFine);
                stmt.setInt(3, currentReturnTransId);
                stmt.executeUpdate();
            }

            String bookSql = "UPDATE books SET is_issued = 0 WHERE book_id = ?";
            try (PreparedStatement stmt = conn.prepareStatement(bookSql)) {
                stmt.setString(1, currentReturnBookId);
                stmt.executeUpdate();
            }

            if (currentCalculatedFine > 0) {
                String fineSql = "UPDATE members SET fine_due = fine_due + ? WHERE member_id = ?";
                try (PreparedStatement stmt = conn.prepareStatement(fineSql)) {
                    stmt.setDouble(1, currentCalculatedFine);
                    stmt.setString(2, currentReturnMemberId);
                    stmt.executeUpdate();
                }
            }

            conn.commit();
            JOptionPane.showMessageDialog(this,
                    "Book returned successfully!\nFine assessed: ₹ " + currentCalculatedFine,
                    "Return Complete", JOptionPane.INFORMATION_MESSAGE);

            refreshReturnDropdown();
            refreshIssueDropdowns();
            loadBooksData();
            loadMembersData();
            refreshDashboardStats();

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error processing return: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // =========================================================================
    // PANEL 5: MEMBER MANAGEMENT (Matching Screenshot d)
    // =========================================================================
    private JPanel createMembersPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(contentBg);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel toolbar = new JPanel(new BorderLayout());
        toolbar.setBackground(contentBg);

        JPanel leftTools = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftTools.setBackground(contentBg);
        leftTools.add(new JLabel("Search Members:"));
        memberSearchField = new JTextField(18);
        memberSearchField.setPreferredSize(new Dimension(180, 32));
        JButton searchBtn = new JButton("Search");
        searchBtn.setBackground(primaryBlue);
        searchBtn.setForeground(Color.BLACK);
        searchBtn.setFocusPainted(false);
        JButton resetBtn = new JButton("Reset");
        resetBtn.setFocusPainted(false);

        leftTools.add(memberSearchField);
        leftTools.add(searchBtn);
        leftTools.add(resetBtn);

        JPanel rightTools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        rightTools.setBackground(contentBg);
        JButton addMemberBtn = new JButton("+ Add New Member");
        addMemberBtn.setBackground(successGreen);
        addMemberBtn.setForeground(Color.BLACK);
        addMemberBtn.setFont(new Font("Segoe UI", Font.BOLD, 13));
        addMemberBtn.setFocusPainted(false);

        JButton deleteMemberBtn = new JButton("Delete Member");
        deleteMemberBtn.setBackground(dangerRed);
        deleteMemberBtn.setForeground(Color.BLACK);
        deleteMemberBtn.setFocusPainted(false);

        rightTools.add(addMemberBtn);
        rightTools.add(deleteMemberBtn);

        toolbar.add(leftTools, BorderLayout.WEST);
        toolbar.add(rightTools, BorderLayout.EAST);
        panel.add(toolbar, BorderLayout.NORTH);

        membersTableModel = new DefaultTableModel(
                new String[]{"Member ID", "Name", "Email", "Fine Due", "Active Issues"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        membersTable = styleTable(new JTable(membersTableModel));
        membersSorter = new TableRowSorter<>(membersTableModel);
        membersTable.setRowSorter(membersSorter);

        panel.add(new JScrollPane(membersTable), BorderLayout.CENTER);

        searchBtn.addActionListener(e -> {
            String text = memberSearchField.getText().trim();
            if (text.isEmpty()) {
                membersSorter.setRowFilter(null);
            } else {
                membersSorter.setRowFilter(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
            }
        });
        resetBtn.addActionListener(e -> {
            memberSearchField.setText("");
            membersSorter.setRowFilter(null);
        });

        addMemberBtn.addActionListener(e -> showAddMemberDialog());
        deleteMemberBtn.addActionListener(e -> handleDeleteMember());

        return panel;
    }

    private void showAddMemberDialog() {
        JTextField idField = new JTextField();
        JTextField nameField = new JTextField();
        JTextField emailField = new JTextField();
        JPasswordField passwordField = new JPasswordField();

        JPanel form = new JPanel(new GridLayout(4, 2, 10, 10));
        form.add(new JLabel("Member ID (e.g. M002):"));
        form.add(idField);
        form.add(new JLabel("Full Name:"));
        form.add(nameField);
        form.add(new JLabel("Email Address:"));
        form.add(emailField);
        form.add(new JLabel("Password:"));
        form.add(passwordField);

        int result = JOptionPane.showConfirmDialog(this, form, "Add New Member",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);

        if (result == JOptionPane.OK_OPTION) {
            String id = idField.getText().trim();
            String name = nameField.getText().trim();
            String email = emailField.getText().trim();
            String pass = new String(passwordField.getPassword());

            if (id.isEmpty() || name.isEmpty() || pass.isEmpty()) {
                JOptionPane.showMessageDialog(this, "ID, Name, and Password are required.");
                return;
            }

            String sql = "INSERT INTO members (member_id, name, email, password, fine_due) VALUES (?, ?, ?, ?, 0.00)";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, id);
                stmt.setString(2, name);
                stmt.setString(3, email.isEmpty() ? null : email);
                stmt.setString(4, pass);
                stmt.executeUpdate();

                JOptionPane.showMessageDialog(this, "Member created successfully!");
                loadMembersData();
                refreshIssueDropdowns();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error adding member: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void handleDeleteMember() {
        int selectedRow = membersTable.getSelectedRow();
        if (selectedRow == -1) {
            JOptionPane.showMessageDialog(this, "Please select a member to delete.");
            return;
        }
        int modelRow = membersTable.convertRowIndexToModel(selectedRow);
        String memberId = (String) membersTableModel.getValueAt(modelRow, 0);
        String name = (String) membersTableModel.getValueAt(modelRow, 1);
        int activeIssues = (int) membersTableModel.getValueAt(modelRow, 4);

        if (activeIssues > 0) {
            JOptionPane.showMessageDialog(this,
                    "Cannot delete member '" + name + "'. They currently have " + activeIssues + " active book(s) issued.\n" +
                    "Books must be returned before deleting the member account.",
                    "Deletion Blocked", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int confirm = JOptionPane.showConfirmDialog(this,
                "Are you sure you want to delete member: " + name + " (" + memberId + ")?",
                "Confirm Deletion", JOptionPane.YES_NO_OPTION);

        if (confirm == JOptionPane.YES_OPTION) {
            String sql = "DELETE FROM members WHERE member_id = ?";
            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {
                stmt.setString(1, memberId);
                stmt.executeUpdate();
                JOptionPane.showMessageDialog(this, "Member deleted successfully.");
                loadMembersData();
                refreshIssueDropdowns();
                refreshDashboardStats();
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Error deleting member: " + ex.getMessage(),
                        "Database Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void loadMembersData() {
        membersTableModel.setRowCount(0);
        String sql = "SELECT m.member_id, m.name, m.email, m.fine_due, " +
                "(SELECT COUNT(*) FROM transactions t WHERE t.member_id = m.member_id AND t.return_date IS NULL) AS active_issues " +
                "FROM members m ORDER BY m.member_id ASC";

        try (Connection conn = DBConnection.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                membersTableModel.addRow(new Object[]{
                        rs.getString("member_id"),
                        rs.getString("name"),
                        rs.getString("email") == null ? "N/A" : rs.getString("email"),
                        "₹ " + rs.getDouble("fine_due"),
                        rs.getInt("active_issues")
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    // =========================================================================
    // PANEL 6: REPORTS MODULE (Issued Books & Overdue Fines)
    // =========================================================================
    private JPanel createReportsPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(contentBg);
        panel.setBorder(new EmptyBorder(25, 25, 25, 25));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 15, 0));
        top.setBackground(contentBg);

        JLabel label = new JLabel("Generate Report:");
        label.setFont(new Font("Segoe UI", Font.BOLD, 14));

        reportIssuedRadio = new JRadioButton("All Currently Issued Books", true);
        reportOverdueRadio = new JRadioButton("Overdue Books & Fines");
        reportIssuedRadio.setBackground(contentBg);
        reportOverdueRadio.setBackground(contentBg);

        ButtonGroup group = new ButtonGroup();
        group.add(reportIssuedRadio);
        group.add(reportOverdueRadio);

        JButton refreshBtn = new JButton("Refresh Report");
        refreshBtn.setBackground(primaryBlue);
        refreshBtn.setForeground(Color.BLACK);
        refreshBtn.setFocusPainted(false);

        top.add(label);
        top.add(reportIssuedRadio);
        top.add(reportOverdueRadio);
        top.add(refreshBtn);

        panel.add(top, BorderLayout.NORTH);

        reportsTableModel = new DefaultTableModel();
        reportsTable = styleTable(new JTable(reportsTableModel));
        panel.add(new JScrollPane(reportsTable), BorderLayout.CENTER);

        refreshBtn.addActionListener(e -> loadReportsData());
        reportIssuedRadio.addActionListener(e -> loadReportsData());
        reportOverdueRadio.addActionListener(e -> loadReportsData());

        return panel;
    }

    private void loadReportsData() {
        reportsTableModel.setRowCount(0);
        boolean isOverdueOnly = reportOverdueRadio.isSelected();

        if (isOverdueOnly) {
            reportsTableModel.setColumnIdentifiers(new String[]{
                    "Trans ID", "Member Name", "Book Title", "Issue Date", "Due Date", "Days Overdue", "Fine (₹2/day)"
            });

            String sql = "SELECT t.transaction_id, m.name, b.title, t.issue_date, t.due_date " +
                    "FROM transactions t " +
                    "JOIN members m ON t.member_id = m.member_id " +
                    "JOIN books b ON t.book_id = b.book_id " +
                    "WHERE t.return_date IS NULL AND t.due_date < ? " +
                    "ORDER BY t.due_date ASC";

            try (Connection conn = DBConnection.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(sql)) {

                LocalDate today = LocalDate.now();
                stmt.setDate(1, Date.valueOf(today));
                ResultSet rs = stmt.executeQuery();

                while (rs.next()) {
                    LocalDate due = rs.getDate("due_date").toLocalDate();
                    long daysOverdue = ChronoUnit.DAYS.between(due, today);
                    long fine = daysOverdue * 2;

                    reportsTableModel.addRow(new Object[]{
                            "T-" + rs.getInt("transaction_id"),
                            rs.getString("name"),
                            rs.getString("title"),
                            rs.getDate("issue_date"),
                            due,
                            daysOverdue + " day(s)",
                            "₹ " + fine
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }

        } else {
            reportsTableModel.setColumnIdentifiers(new String[]{
                    "Trans ID", "Member Name", "Book Title", "Issue Date", "Due Date", "Status"
            });

            String sql = "SELECT t.transaction_id, m.name, b.title, t.issue_date, t.due_date " +
                    "FROM transactions t " +
                    "JOIN members m ON t.member_id = m.member_id " +
                    "JOIN books b ON t.book_id = b.book_id " +
                    "WHERE t.return_date IS NULL " +
                    "ORDER BY t.issue_date DESC";

            try (Connection conn = DBConnection.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {

                LocalDate today = LocalDate.now();
                while (rs.next()) {
                    LocalDate due = rs.getDate("due_date").toLocalDate();
                    String status = today.isAfter(due) ? "OVERDUE" : "Within Term";
                    reportsTableModel.addRow(new Object[]{
                            "T-" + rs.getInt("transaction_id"),
                            rs.getString("name"),
                            rs.getString("title"),
                            rs.getDate("issue_date"),
                            due,
                            status
                    });
                }
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }
    }

    private JTable styleTable(JTable table) {
        table.setRowHeight(32);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 13));
        table.setSelectionBackground(new Color(219, 234, 254));
        table.setSelectionForeground(textDark);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setBackground(new Color(243, 244, 246));
        table.getTableHeader().setForeground(new Color(55, 65, 81));
        table.getTableHeader().setReorderingAllowed(false);
        table.setShowGrid(true);
        table.setGridColor(new Color(243, 244, 246));

        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        table.setDefaultRenderer(Object.class, centerRenderer);

        return table;
    }
}
