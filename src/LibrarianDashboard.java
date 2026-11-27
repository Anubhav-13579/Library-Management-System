import java.awt.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.regex.Pattern;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.plaf.basic.BasicButtonUI;
import javax.swing.table.*;

public class LibrarianDashboard extends JFrame {
    private static final Color PAPER = Color.WHITE;
    private static final Color CANVAS = new Color(0xF5F7FB);
    private static final Color ROSE = new Color(0xDBE9FF);
    private static final Color WINE = new Color(0x2449A6);
    private static final Color TEXT = new Color(0x172B42);
    private static final Color MUTED = new Color(0x536478);
    private static final Color LINE = new Color(0xDBE3EB);
    private static final String[] VIEWS = {"Overview", "Books", "Members", "Current loans", "Overdue"};
    private static final String[] STATS = {"Books", "Members", "Current loans", "Overdue"};
    private static final String[][] HEADERS = {
        {"Transaction ID", "Book", "Member", "Issue date", "Due date", "Return date", "Status"},
        {"Book ID", "Title", "Author", "Category", "Availability"},
        {"Member ID", "Name", "Email", "Current loans"},
        {"Transaction ID", "Book", "Member", "Issue date", "Due date"},
        {"Transaction ID", "Book", "Member", "Due date", "Days late", "Accrued fine (₹)"}
    };
    private final LibraryDesk desk = new LibraryDesk(this);
    private final JPanel actions = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
    private int view;
    private int generation;
    private final JLabel title = label("", 26, true);
    private final JLabel subtitle = label("", 16, true);
    private final JLabel status = label("", 13, false);
    private final JLabel[] values = new JLabel[4];
    private final JButton[] navigation = new JButton[VIEWS.length];
    private final JPanel stats = new JPanel(new GridLayout(1, 4, 12, 0));
    private final JTable table = new JTable();
    private final JTextField search = new JTextField(16);
    private final JButton refresh = button("Refresh");

    public LibrarianDashboard(CurrentUser user) {
        super("Librarian Dashboard");
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        buildAdmin(user);
        selectView(0);
        setLocationRelativeTo(null);
    }

    private void buildAdmin(CurrentUser user) {
        JPanel root = new JPanel(new BorderLayout());
        JPanel side = surface(DashboardStyle.NAVY, 16);
        side.setPreferredSize(new Dimension(194, 0));
        JPanel menu = new JPanel(new GridLayout(0, 1, 0, 6));
        menu.setOpaque(false);
        JLabel brand = label("Digital Library", 20, true);
        brand.setForeground(Color.WHITE);
        menu.add(brand);
        JLabel campus = label("NIET Greater Noida", 13, false);
        campus.setForeground(new Color(0xBBCBE0));
        menu.add(campus);
        for (int i = 0; i < VIEWS.length; i++) {
            final int target = i;
            JButton link = button(VIEWS[i]);
            link.setForeground(Color.WHITE);
            link.setBorder(new EmptyBorder(9, 14, 9, 10));
            link.setHorizontalAlignment(SwingConstants.LEFT);
            link.addActionListener(e -> selectView(target));
            navigation[i] = link;
            menu.add(link);
        }
        side.add(menu, BorderLayout.NORTH);
        side.add(logout(), BorderLayout.SOUTH);
        root.add(side, BorderLayout.WEST);

        JPanel main = surface(CANVAS, 20);
        JPanel heading = surface(CANVAS, 0);
        heading.add(title, BorderLayout.WEST);
        JPanel tools = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        tools.setOpaque(false);
        tools.add(label(user.name, 14, false));
        tools.add(refresh);
        refresh.addActionListener(e -> reload());
        heading.add(tools, BorderLayout.EAST);
        JPanel top = surface(CANVAS, 0);
        top.add(heading, BorderLayout.NORTH);
        stats.setOpaque(false);
        for (int i = 0; i < values.length; i++) {
            JPanel card = DashboardStyle.card(16);
            card.add(label(STATS[i], 14, false), BorderLayout.NORTH);
            values[i] = label("—", 30, true);
            values[i].setForeground(DashboardStyle.ACCENTS[i]);
            card.add(values[i]);
            stats.add(card);
        }
        top.add(stats);
        main.add(top, BorderLayout.NORTH);

        JPanel content = DashboardStyle.card(16);
        JPanel bar = surface(PAPER, 0);
        bar.add(subtitle, BorderLayout.WEST);
        JPanel filter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 5, 0));
        filter.setOpaque(false);
        JLabel caption = label("Search", 13, false);
        caption.setLabelFor(search);
        filter.add(caption);
        filter.add(search);
        bar.add(filter, BorderLayout.EAST);
        JPanel toolbar = surface(PAPER, 0);
        toolbar.add(bar, BorderLayout.NORTH);
        actions.setOpaque(false);
        toolbar.add(actions, BorderLayout.SOUTH);
        content.add(toolbar, BorderLayout.NORTH);
        table.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        table.setForeground(TEXT);
        table.setBackground(PAPER);
        table.setSelectionBackground(ROSE);
        table.setSelectionForeground(TEXT);
        table.setGridColor(LINE);
        table.setRowHeight(38);
        table.setShowVerticalLines(false);
        table.setFillsViewportHeight(true);
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setBackground(new Color(0xEBF0F7));
        table.getTableHeader().setForeground(TEXT);
        table.getTableHeader().setFont(new Font("Segoe UI", Font.BOLD, 13));
        table.getTableHeader().setPreferredSize(new Dimension(0, 36));
        JScrollPane scroll = new JScrollPane(table);
        scroll.setBorder(new EmptyBorder(0, 0, 0, 0));
        scroll.getViewport().setBackground(PAPER);
        content.add(scroll);
        status.setForeground(MUTED);
        content.add(status, BorderLayout.SOUTH);
        main.add(content);
        root.add(main);
        setContentPane(root);
        search.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { applyFilter(); }
            public void removeUpdate(DocumentEvent e) { applyFilter(); }
            public void changedUpdate(DocumentEvent e) { applyFilter(); }
        });
        setSize(1160, 670);
        setMinimumSize(new Dimension(820, 480));
    }

    private void selectView(int target) {
        view = target;
        setActions(target);
        title.setText(target == 0 ? "Librarian overview" : VIEWS[target]);
        subtitle.setText(target == 0 ? "Recent library transactions" :
                new String[]{"", "Book catalogue", "Member directory", "Issued books", "Overdue loans"}[target]);
        stats.setVisible(target == 0);
        for (int i = 0; i < navigation.length; i++) navigation[i].setBackground(i == target ? DashboardStyle.BLUE : DashboardStyle.NAVY);
        search.setText("");
        reload();
    }

    private interface Action { void run() throws Exception; }
    private void action(String caption, Action action) {
        JButton b = button(caption);
        b.addActionListener(e -> {
            try { action.run(); reload(); }
            catch (Exception ex) { desk.info(ex.getMessage()); }
        });
        actions.add(b);
    }
    private String selectedId() {
        int row = table.getSelectedRow();
        if (row < 0) throw new IllegalArgumentException("Select a row in the table first.");
        return table.getValueAt(row, 0).toString();
    }
    private void setActions(int target) {
        actions.removeAll();
        switch (target) {
            case 0 -> {
                action("Issue book", desk::issue);
                action("Borrowing history", () -> desk.report("Borrowing history", HEADERS[0],
                        LibraryDesk.read(DashboardRepository.HISTORY), "All library transactions"));
                action("Daily collections", desk::daily);
            }
            case 1 -> {
                action("Add book", () -> desk.book(null));
                action("Edit", () -> desk.book(selectedId()));
                action("Delete", () -> desk.remove(true, selectedId()));
                action("Issue book", desk::issue);
            }
            case 2 -> {
                action("Register member", () -> desk.member(null));
                action("Edit", () -> desk.member(selectedId()));
                action("Delete", () -> desk.remove(false, selectedId()));
                action("Collect fine", () -> desk.collect(selectedId(), false));
                action("History", () -> {
                    String id = selectedId();
                    desk.report("Borrowing history · "+id, HEADERS[0], LibraryDesk.read(
                        DashboardRepository.HISTORY_BY_MEMBER, id), "Member "+id);
                });
            }
            default -> {
                action("Edit due date", () -> desk.editDueDate(selectedId()));
                action("Return selected book", () -> desk.giveBack(selectedId()));
                action("Export CSV", () -> LibraryDesk.export(this, table));
                action("Print / PDF", () -> table.print(JTable.PrintMode.FIT_WIDTH));
            }
        }
        actions.revalidate(); actions.repaint();
    }

    private void reload() {
        final int ticket = ++generation;
        final int target = view;
        refresh.setEnabled(false);
        search.setEnabled(false);
        status.setText("Loading…");
        setRows(target, List.of());
        for (JLabel value : values) value.setText("—");
        new SwingWorker<DashboardRepository.Snapshot, Void>() {
            protected DashboardRepository.Snapshot doInBackground() throws SQLException {
                return DashboardRepository.load(target);
            }
            protected void done() {
                if (ticket != generation) return;
                refresh.setEnabled(true);
                try {
                    DashboardRepository.Snapshot data = get();
                    setRows(target, data.rows());
                    if (target == 0) for (int i = 0; i < values.length; i++)
                        values[i].setText(Integer.toString(data.counts()[i]));
                    search.setEnabled(true);
                    applyFilter();
                } catch (ExecutionException ex) {
                    status.setText("Could not load records. Check MySQL, then click Refresh.");
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                    status.setText("Loading interrupted. Click Refresh to retry.");
                }
            }
        }.execute();
    }

    private void setRows(int target, List<Object[]> rows) {
        DefaultTableModel model = new DefaultTableModel(rows.toArray(Object[][]::new), HEADERS[target]) {
            public boolean isCellEditable(int row, int column) { return false; }
            public Class<?> getColumnClass(int column) {
                for (int r = 0; r < getRowCount(); r++) {
                    Object value = getValueAt(r, column);
                    if (value != null) return value.getClass();
                }
                return Object.class;
            }
        };
        table.setModel(model);
        for (int i = 0; i < HEADERS[target].length; i++) {
            String name = HEADERS[target][i];
            TableColumn col = table.getColumnModel().getColumn(i);
            if (name.equals("Transaction ID")) col.setMinWidth(110);
            if (name.equals("Status") || name.equals("Availability")) {
                col.setMinWidth(90);
                col.setCellRenderer(new DashboardStyle.StatusCell());
            }
            if (name.equals("Book") || name.equals("Title")) col.setPreferredWidth(155);
        }
        table.setRowSorter(new TableRowSorter<>(model));
    }

    private void applyFilter() {
        if (!(table.getRowSorter() instanceof TableRowSorter<?> sorter)) return;
        String text = search.getText().strip();
        sorter.setRowFilter(text.isEmpty() ? null : RowFilter.regexFilter("(?iu)" + Pattern.quote(text)));
        int count = table.getRowCount();
        status.setText(count == 0 ? (text.isEmpty() ? "No records yet." : "No matching records.")
                : count + (count == 1 ? " record" : " records"));
    }

    private JButton logout() {
        JButton b = button("Log out");
        b.setBackground(new Color(0x8F3143));
        b.setForeground(Color.WHITE);
        b.setBorder(new EmptyBorder(9, 10, 9, 10));
        b.addActionListener(e -> { generation++; dispose(); new LoginFrame().setVisible(true); });
        return b;
    }
    private static JPanel surface(Color color, int padding) {
        JPanel panel = new JPanel(new BorderLayout(10, 14));
        panel.setBackground(color);
        panel.setBorder(new EmptyBorder(padding, padding, padding, padding));
        return panel;
    }
    private static JLabel label(String text, int size, boolean bold) {
        JLabel label = new JLabel(text);
        label.setFont(new Font("Segoe UI", bold ? Font.BOLD : Font.PLAIN, size));
        label.setForeground(TEXT);
        return label;
    }
    private static JButton button(String text) {
        JButton b = new JButton(text);
        b.setUI(new BasicButtonUI());
        b.setOpaque(true);
        b.setBackground(PAPER);
        b.setForeground(WINE);
        b.setFont(new Font("Segoe UI", Font.BOLD, 14));
        b.setBorder(new CompoundBorder(new LineBorder(LINE), new EmptyBorder(7, 10, 7, 10)));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return b;
    }
}
