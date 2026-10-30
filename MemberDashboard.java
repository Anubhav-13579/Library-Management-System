import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableRowSorter;
import java.awt.*;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * MemberDashboard - Student & Faculty Portal
 * Provides restricted, role-based access for library members:
 * - Search the book catalog
 * - View personal active issues, due dates, and fine balances
 */
public class MemberDashboard extends JFrame {

    private final CurrentUser currentUser;
    private JLabel totalFineBadge;
    private JLabel activeIssuesBadge;

    private DefaultTableModel myBooksModel;
    private JTable myBooksTable;

    private DefaultTableModel catalogModel;
    private JTable catalogTable;
    private TableRowSorter<DefaultTableModel> catalogSorter;
    private JTextField catalogSearchField;
    private JComboBox<String> catalogCategoryFilter;

    // Colors
    private final Color headerNavy = new Color(26, 54, 93);       // #1A365D
    private final Color primaryBlue = new Color(29, 78, 216);     // #1D4ED8
    private final Color bgLight = new Color(241, 245, 249);       // #F1F5F9
    private final Color borderGray = new Color(226, 232, 240);    // #E2E8F0
    private final Color textDark = new Color(30, 41, 59);         // #1E293B

    public MemberDashboard(CurrentUser currentUser) {
        this.currentUser = currentUser;

        setTitle("Digital Library Management System - Member Portal");
        setSize(960, 620);
        setMinimumSize(new Dimension(850, 520));
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout());

        // 1. Top Header & Welcome Banner
        add(createTopBanner(), BorderLayout.NORTH);

        // 2. Tabbed Content (My Books vs Browse Catalog)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(new Font("Segoe UI", Font.BOLD, 13));
        tabbedPane.setBackground(Color.WHITE);
        tabbedPane.addTab("📖  My Borrowed Books", createMyBooksPanel());
        tabbedPane.addTab("🔍  Browse Library Catalog", createCatalogPanel());

        add(tabbedPane, BorderLayout.CENTER);

        // Initial Data Load
        loadMyBooks();
        loadCatalogData();
    }

    private JPanel createTopBanner() {
        JPanel header = new JPanel(new BorderLayout());
        header.setBackground(headerNavy);
        header.setBorder(new EmptyBorder(16, 24, 16, 24));

        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setBackground(headerNavy);

        JLabel welcomeLbl = new JLabel("Welcome, " + currentUser.name);
        welcomeLbl.setFont(new Font("Segoe UI", Font.BOLD, 18));
        welcomeLbl.setForeground(Color.WHITE);

        JLabel subLbl = new JLabel("Member ID: " + currentUser.id + "  |  NIET Digital Library");
        subLbl.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        subLbl.setForeground(new Color(203, 213, 225));

        left.add(welcomeLbl);
        left.add(Box.createVerticalStrut(3));
        left.add(subLbl);
        header.add(left, BorderLayout.WEST);

        JPanel right = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 5));
        right.setBackground(headerNavy);

        activeIssuesBadge = createBadge("Issued Books: 0", new Color(30, 58, 138));
        totalFineBadge = createBadge("Fine Due: ₹ 0", new Color(185, 28, 28));

        JButton logoutBtn = new JButton("Logout");
        logoutBtn.setBackground(new Color(239, 68, 68));
        logoutBtn.setForeground(Color.WHITE);
        logoutBtn.setFont(new Font("Segoe UI", Font.BOLD, 12));
        logoutBtn.setFocusPainted(false);
        logoutBtn.setCursor(new Cursor(Cursor.HAND_CURSOR));
        logoutBtn.addActionListener(e -> {
            new LoginFrame().setVisible(true);
            this.dispose();
        });

        right.add(activeIssuesBadge);
        right.add(totalFineBadge);
        right.add(logoutBtn);
        header.add(right, BorderLayout.EAST);

        return header;
    }

    private JLabel createBadge(String text, Color bg) {
        JLabel badge = new JLabel(text);
        badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
        badge.setForeground(Color.WHITE);
        badge.setBackground(bg);
        badge.setOpaque(true);
        badge.setBorder(new EmptyBorder(6, 12, 6, 12));
        return badge;
    }

    // =========================================================================
    // TAB 1: MY BORROWED BOOKS
    // =========================================================================
    private JPanel createMyBooksPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(bgLight);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel topBar = new JPanel(new BorderLayout());
        topBar.setBackground(bgLight);

        JLabel title = new JLabel("Your Current & Past Borrowing History");
        title.setFont(new Font("Segoe UI", Font.BOLD, 15));
        title.setForeground(textDark);
        topBar.add(title, BorderLayout.WEST);

        JButton refreshBtn = new JButton("Refresh");
        refreshBtn.setBackground(primaryBlue);
        refreshBtn.setForeground(Color.WHITE);
        refreshBtn.setFocusPainted(false);
        refreshBtn.addActionListener(e -> loadMyBooks());
        topBar.add(refreshBtn, BorderLayout.EAST);

        panel.add(topBar, BorderLayout.NORTH);

        myBooksModel = new DefaultTableModel(
                new String[]{"Book Title", "Issue Date", "Due Date", "Return Date", "Status", "Fine (₹)"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        myBooksTable = styleTable(new JTable(myBooksModel));
        panel.add(new JScrollPane(myBooksTable), BorderLayout.CENTER);

        return panel;
    }

    private void loadMyBooks() {
        myBooksModel.setRowCount(0);
        int totalFine = 0;
        int activeCount = 0;

        String query =
                "SELECT b.title AS book_title, t.issue_date, t.due_date, t.return_date, t.fine_amount " +
                "FROM transactions t " +
                "JOIN books b ON t.book_id = b.book_id " +
                "WHERE t.member_id = ? " +
                "ORDER BY t.transaction_id DESC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, currentUser.id);
            ResultSet rs = stmt.executeQuery();
            LocalDate today = LocalDate.now();

            while (rs.next()) {
                Date issueDate = rs.getDate("issue_date");
                Date dueDate = rs.getDate("due_date");
                Date returnDate = rs.getDate("return_date");

                String status;
                int fine = 0;

                if (returnDate != null) {
                    status = "Returned";
                    fine = (int) rs.getDouble("fine_amount");
                } else {
                    activeCount++;
                    LocalDate due = dueDate.toLocalDate();
                    if (today.isAfter(due)) {
                        long overdueDays = ChronoUnit.DAYS.between(due, today);
                        fine = (int) (overdueDays * 2);
                        status = "OVERDUE (" + overdueDays + " days)";
                    } else {
                        status = "Active Borrow";
                    }
                }

                totalFine += fine;

                myBooksModel.addRow(new Object[]{
                        rs.getString("book_title"),
                        issueDate,
                        dueDate,
                        returnDate == null ? "Not returned" : returnDate,
                        status,
                        fine > 0 ? "₹ " + fine : "₹ 0"
                });
            }

            activeIssuesBadge.setText("Issued Books: " + activeCount);
            totalFineBadge.setText("Fine Due: ₹ " + totalFine);

        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Error loading borrowing history: " + ex.getMessage());
        }
    }

    // =========================================================================
    // TAB 2: BROWSE CATALOG
    // =========================================================================
    private JPanel createCatalogPanel() {
        JPanel panel = new JPanel(new BorderLayout(15, 15));
        panel.setBackground(bgLight);
        panel.setBorder(new EmptyBorder(20, 20, 20, 20));

        JPanel toolbar = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        toolbar.setBackground(bgLight);

        JLabel searchLbl = new JLabel("Search Books:");
        searchLbl.setFont(new Font("Segoe UI", Font.BOLD, 13));
        catalogSearchField = new JTextField(18);
        catalogSearchField.setPreferredSize(new Dimension(200, 32));

        catalogCategoryFilter = new JComboBox<>(new String[]{"All Categories", "Computer Science", "Fiction", "Mathematics", "Science", "General"});
        catalogCategoryFilter.setPreferredSize(new Dimension(150, 32));

        JButton filterBtn = new JButton("Filter");
        filterBtn.setBackground(primaryBlue);
        filterBtn.setForeground(Color.WHITE);
        filterBtn.setFocusPainted(false);

        JButton resetBtn = new JButton("Reset");
        resetBtn.setFocusPainted(false);

        toolbar.add(searchLbl);
        toolbar.add(catalogSearchField);
        toolbar.add(catalogCategoryFilter);
        toolbar.add(filterBtn);
        toolbar.add(resetBtn);

        panel.add(toolbar, BorderLayout.NORTH);

        catalogModel = new DefaultTableModel(
                new String[]{"Book ID", "Title", "Author", "Category", "Availability"}, 0) {
            @Override
            public boolean isCellEditable(int r, int c) { return false; }
        };
        catalogTable = styleTable(new JTable(catalogModel));
        catalogSorter = new TableRowSorter<>(catalogModel);
        catalogTable.setRowSorter(catalogSorter);

        panel.add(new JScrollPane(catalogTable), BorderLayout.CENTER);

        Runnable applyFilter = () -> {
            String text = catalogSearchField.getText().trim();
            String cat = (String) catalogCategoryFilter.getSelectedItem();
            List<RowFilter<Object, Object>> filters = new ArrayList<>();

            if (!text.isEmpty()) {
                filters.add(RowFilter.regexFilter("(?i)" + text, 0, 1, 2));
            }
            if (cat != null && !cat.equals("All Categories")) {
                filters.add(RowFilter.regexFilter("(?i)^" + cat + "$", 3));
            }

            if (filters.isEmpty()) {
                catalogSorter.setRowFilter(null);
            } else {
                catalogSorter.setRowFilter(RowFilter.andFilter(filters));
            }
        };

        filterBtn.addActionListener(e -> applyFilter.run());
        resetBtn.addActionListener(e -> {
            catalogSearchField.setText("");
            catalogCategoryFilter.setSelectedIndex(0);
            catalogSorter.setRowFilter(null);
        });

        return panel;
    }

    private void loadCatalogData() {
        catalogModel.setRowCount(0);
        String sql = "SELECT book_id, title, author, category, is_issued FROM books ORDER BY title ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                boolean isIssued = rs.getBoolean("is_issued");
                catalogModel.addRow(new Object[]{
                        rs.getString("book_id"),
                        rs.getString("title"),
                        rs.getString("author"),
                        rs.getString("category"),
                        isIssued ? "Checked Out" : "Available in Library"
                });
            }
        } catch (Exception ex) {
            ex.printStackTrace();
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
