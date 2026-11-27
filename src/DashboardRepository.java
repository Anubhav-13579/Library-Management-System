import java.sql.*;
import java.util.ArrayList;
import java.util.List;

final class DashboardRepository {
    private static final String JOIN = " FROM transactions t JOIN books b ON b.book_id=t.book_id "
            + "JOIN members m ON m.member_id=t.member_id ";
    private static final String OPEN = "t.return_date IS NULL";
    private static final String LATE = OPEN + " AND t.due_date < CURRENT_DATE";
    private static final String HISTORY_SELECT = "SELECT t.transaction_id,b.title,m.name,t.issue_date,t.due_date,t.return_date,"
            + "CASE WHEN t.return_date IS NOT NULL THEN 'Returned' WHEN t.due_date<CURRENT_DATE THEN 'Overdue' ELSE 'Issued' END" + JOIN;
    private static final String ORDER = " ORDER BY COALESCE(t.return_date,t.issue_date) DESC,t.transaction_id DESC";
    static final String HISTORY = HISTORY_SELECT + ORDER;
    static final String HISTORY_BY_MEMBER = HISTORY_SELECT + " WHERE t.member_id=?" + ORDER;
    private static final String[] QUERIES = {
        HISTORY + " LIMIT 10",
        "SELECT book_id,title,author,category,CASE WHEN is_issued THEN 'Issued' ELSE 'Available' END "
            + "FROM books ORDER BY title,book_id",
        "SELECT m.member_id,m.name,m.email,(SELECT COUNT(*) FROM transactions t WHERE "
            + "t.member_id=m.member_id AND " + OPEN + ") FROM members m ORDER BY m.name,m.member_id",
        "SELECT t.transaction_id,b.title,m.name,t.issue_date,t.due_date" + JOIN
            + "WHERE " + OPEN + " ORDER BY t.due_date,t.transaction_id",
        "SELECT t.transaction_id,b.title,m.name,t.due_date,DATEDIFF(CURRENT_DATE,t.due_date),"
            + "2*DATEDIFF(CURRENT_DATE,t.due_date)" + JOIN + "WHERE " + LATE + " ORDER BY t.due_date,t.transaction_id"
    };
    private static final String COUNTS = "SELECT (SELECT COUNT(*) FROM books),(SELECT COUNT(*) FROM members),"
            + "(SELECT COUNT(*) FROM transactions t WHERE " + OPEN + "),"
            + "(SELECT COUNT(*) FROM transactions t WHERE " + LATE + ")";

    record Snapshot(int[] counts, List<Object[]> rows) {}

    static Snapshot load(int target) throws SQLException {
        List<Object[]> rows = new ArrayList<>();
        int[] counts = new int[4];
        try (Connection c = DBConnection.getConnection(); Statement s = c.createStatement()) {
            if (target == 0) {
                try (ResultSet r = s.executeQuery(COUNTS)) {
                    if (r.next()) for (int i = 0; i < counts.length; i++) counts[i] = r.getInt(i + 1);
                }
            }
            try (ResultSet r = s.executeQuery(QUERIES[target])) {
                while (r.next()) {
                    Object[] row = new Object[r.getMetaData().getColumnCount()];
                    for (int i = 0; i < row.length; i++) row[i] = r.getObject(i + 1);
                    rows.add(row);
                }
            }
        }
        return new Snapshot(counts, rows);
    }
}
