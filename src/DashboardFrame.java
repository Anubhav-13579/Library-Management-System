import javax.swing.JFrame;

public final class DashboardFrame {
    public static JFrame forUser(CurrentUser user) {
        return switch (user.role) {
            case "Librarian" -> new LibrarianDashboard(user);
            case "Member" -> new MemberDashboard(user);
            default -> throw new IllegalArgumentException("Unknown library role");
        };
    }
}
