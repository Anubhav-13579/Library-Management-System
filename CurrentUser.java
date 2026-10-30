public class CurrentUser {
    public final String id;
    public final String name;
    public final String role; // "Librarian" or "Member"

    public CurrentUser(String id, String name, String role) {
        this.id = id;
        this.name = name;
        this.role = role;
    }
}
