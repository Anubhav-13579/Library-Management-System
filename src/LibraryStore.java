import java.sql.*;
import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

final class LibraryStore {
    record Choice(String id, String label) { public String toString() { return label; } }
    interface Work<T> { T run(Connection connection) throws SQLException; }

    static <T> T commit(Work<T> work) throws SQLException {
        try (Connection c = DBConnection.getConnection()) {
            c.setAutoCommit(false);
            try {
                T result = work.run(c);
                c.commit();
                return result;
            } catch (SQLException | RuntimeException e) {
                c.rollback();
                throw e;
            }
        }
    }
    static PreparedStatement prepare(Connection c, String sql, Object... args) throws SQLException {
        PreparedStatement p = c.prepareStatement(sql);
        try {
            for (int i=0; i<args.length; i++) p.setObject(i+1,args[i]);
            return p;
        } catch (SQLException ex) { p.close(); throw ex; }
    }
    static List<Object[]> rows(Connection c, String sql, Object... args) throws SQLException {
        var result = new ArrayList<Object[]>();
        try (var p=prepare(c,sql,args); var r=p.executeQuery()) {
            int n=r.getMetaData().getColumnCount();
            while (r.next()) {
                Object[] row=new Object[n];
                for(int i=0;i<n;i++) row[i]=r.getObject(i+1);
                result.add(row);
            }
        }
        return result;
    }
    static Object[] one(Connection c,String sql,Object... args) throws SQLException {
        var data=rows(c,sql,args);
        if(data.isEmpty()) throw new SQLException("Record unavailable. Refresh and try again.");
        return data.get(0);
    }
    static void update(Connection c,String sql,Object... args) throws SQLException {
        try(var p=prepare(c,sql,args)){p.executeUpdate();}
    }
    static String register(String name,String email,String password) throws SQLException {
        return commit(c -> {
            for(int attempt=0;attempt<40;attempt++) {
                String id="MEM-"+ThreadLocalRandom.current().nextInt(100000,1000000);
                if(!rows(c,"SELECT librarian_id FROM librarians WHERE librarian_id=?",id).isEmpty()) continue;
                try {
                    update(c,"INSERT INTO members(member_id,name,email,password,fine_due) VALUES(?,?,?,?,0)",id,name,email,password);
                    return id;
                } catch(SQLException ex){ if(ex.getErrorCode()!=1062) throw ex; }
            }
            throw new SQLException("Could not allocate an ID. Please retry.");
        });
    }
    static void validateDates(LocalDate issued, LocalDate due, LocalDate today) {
        if (issued.isAfter(today)) throw new IllegalArgumentException("The issue date cannot be in the future.");
        if (due.isBefore(issued)) throw new IllegalArgumentException("The due date must be on or after the issue date.");
        if (issued.getYear() < 1000 || due.getYear() > 9999)
            throw new IllegalArgumentException("Use dates between years 1000 and 9999.");
    }

    static void issue(String member, String book, LocalDate issued, LocalDate due) throws SQLException {
        commit(c -> {
            validateDates(issued, due, LocalDate.parse(one(c, "SELECT CURRENT_DATE")[0].toString()));
            one(c,"SELECT member_id FROM members WHERE member_id=? FOR UPDATE",member);
            one(c,"SELECT book_id FROM books WHERE book_id=? AND is_issued=0 FOR UPDATE",book);
            if(!rows(c,"SELECT transaction_id FROM transactions WHERE book_id=? AND return_date IS NULL FOR UPDATE",book).isEmpty())
                throw new SQLException("This copy already has an active loan.");
            update(c,"INSERT INTO transactions(member_id,book_id,issue_date,due_date) VALUES(?,?,?,?)",member,book,issued,due);
            update(c,"UPDATE books SET is_issued=1 WHERE book_id=?",book);
            return null;
        });
    }
    static void changeDueDate(String id, LocalDate original, LocalDate due) throws SQLException {
        commit(c -> {
            Object[] loan = one(c, "SELECT issue_date,due_date,CURRENT_DATE FROM transactions "
                    + "WHERE transaction_id=? AND return_date IS NULL FOR UPDATE", id);
            if (!original.equals(LocalDate.parse(loan[1].toString())))
                throw new SQLException("The due date has changed. Refresh and try again.");
            validateDates(LocalDate.parse(loan[0].toString()), due, LocalDate.parse(loan[2].toString()));
            update(c, "UPDATE transactions SET due_date=? WHERE transaction_id=?", due, id);
            return null;
        });
    }

    static BigDecimal returnBook(String id, LocalDate expectedDue) throws SQLException {
        return commit(c -> {
            Object[] row=one(c,"SELECT member_id,book_id,2*GREATEST(0,DATEDIFF(CURRENT_DATE,due_date)),due_date FROM transactions WHERE transaction_id=? AND return_date IS NULL FOR UPDATE",id);
            if (!expectedDue.equals(LocalDate.parse(row[3].toString())))
                throw new SQLException("The due date has changed. Reopen the return to review the fine.");
            var fine=new BigDecimal(row[2].toString());
            update(c,"UPDATE transactions SET return_date=CURRENT_DATE,fine_amount=? WHERE transaction_id=?",fine,id);
            update(c,"UPDATE books SET is_issued=0 WHERE book_id=?",row[1]);
            if(fine.signum()>0){
                update(c,"INSERT INTO library_charges(transaction_id,amount) VALUES(?,?)",id,fine);
                update(c,"UPDATE members SET fine_due=COALESCE(fine_due,0)+? WHERE member_id=?",fine,row[0]);
            }
            return fine;
        });
    }
    static final String UNPAID=" FROM library_charges f JOIN transactions t ON t.transaction_id=f.transaction_id LEFT JOIN library_payments p ON p.transaction_id=f.transaction_id WHERE p.receipt_id IS NULL AND t.member_id=?";
    static List<Choice> dues(Connection c,String member) throws SQLException {
        BigDecimal balance=new BigDecimal(one(c,"SELECT COALESCE(fine_due,0) FROM members WHERE member_id=?",member)[0].toString());
        var list=new ArrayList<Choice>();
        for(Object[] r:rows(c,"SELECT f.transaction_id,f.amount"+UNPAID,member)){
            list.add(new Choice(r[0].toString(),"Transaction "+r[0]+"  ·  ₹"+r[1]));
            balance=balance.subtract(new BigDecimal(r[1].toString()));
        }
        if(balance.signum()>0) list.add(new Choice("legacy","Previous account balance  ·  ₹"+balance));
        return list;
    }
    static String pay(String member,Choice due,String method) throws SQLException {
        return commit(c -> {
            BigDecimal balance=new BigDecimal(one(c,"SELECT COALESCE(fine_due,0) FROM members WHERE member_id=? FOR UPDATE",member)[0].toString());
            if(!dues(c,member).contains(due)) throw new SQLException("This balance changed. Refresh before collecting.");
            boolean legacy=due.id.equals("legacy");
            Object value=legacy?one(c,"SELECT COALESCE(SUM(f.amount),0)"+UNPAID,member)[0]:
                one(c,"SELECT f.amount"+UNPAID+" AND f.transaction_id=?",member,due.id)[0];
            BigDecimal amount=new BigDecimal(value.toString());
            if(legacy) amount=balance.subtract(amount);
            if(amount.signum()<=0 || amount.compareTo(balance)>0) throw new SQLException("Check the member balance before collecting.");
            update(c,"INSERT INTO library_payments(member_id,transaction_id,amount,method) VALUES(?,?,?,?)",member,legacy?null:due.id,amount,method);
            Object receipt=one(c,"SELECT LAST_INSERT_ID()")[0];
            update(c,"UPDATE members SET fine_due=COALESCE(fine_due,0)-? WHERE member_id=?",amount,member);
            return "Receipt "+receipt+"   ·   ₹"+amount+"   ·   "+method;
        });
    }
}
