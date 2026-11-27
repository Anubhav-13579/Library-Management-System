import java.awt.*;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableCellRenderer;

final class DashboardStyle {
    static final Color NAVY = new Color(0x172334), BLUE = new Color(0x2449A6);
    static final Color[] ACCENTS = {new Color(0x315FC6), new Color(0x087760),
            new Color(0xA35A08), new Color(0xB5364B)};
    private DashboardStyle() {}

    static JPanel card(int padding) {
        JPanel p = new JPanel(new BorderLayout(10, 10)) {
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                Graphics2D pen = (Graphics2D)g.create();
                pen.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                pen.setColor(Color.WHITE);
                pen.fillRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                pen.setColor(new Color(0xDCE3EB));
                pen.drawRoundRect(0, 0, getWidth()-1, getHeight()-1, 18, 18);
                pen.dispose();
            }
        };
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(padding,padding,padding,padding));
        return p;
    }

    static final class StatusCell extends JPanel implements TableCellRenderer {
        private final JLabel badge = new JLabel();
        StatusCell() {
            super(new FlowLayout(FlowLayout.LEFT, 5, 6));
            badge.setOpaque(true);
            badge.setFont(new Font("Segoe UI", Font.BOLD, 12));
            badge.setBorder(new EmptyBorder(3, 8, 3, 8));
            add(badge);
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean selected,
                boolean focus, int row, int column) {
            String status = String.valueOf(value);
            badge.setText(status);
            boolean good = status.equals("Returned") || status.equals("Available");
            boolean late = status.equals("Overdue");
            badge.setForeground(late ? ACCENTS[3] : good ? ACCENTS[1] : ACCENTS[0]);
            badge.setBackground(new Color(late ? 0xFCE3E8 : good ? 0xDDF4EA : 0xE3ECFF));
            setBackground(selected ? table.getSelectionBackground() : Color.WHITE);
            return this;
        }
    }
}
