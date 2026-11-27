import java.awt.*;
import java.sql.*;
import java.time.LocalDate;
import java.util.List;
import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.nio.file.Files;
import java.util.stream.Collectors;

final class LibraryDesk {
    private final Component owner;
    LibraryDesk(Component owner){this.owner=owner;}
    boolean form(String title,Object... pairs) {
        JPanel panel=new JPanel(new GridLayout(0,2,12,9));
        for(int i=0;i<pairs.length;i+=2){
            JLabel label=new JLabel(pairs[i].toString());
            Component input=(Component)pairs[i+1];
            label.setFont(input.getFont());
            panel.add(label);panel.add(input);
        }
        return JOptionPane.showConfirmDialog(owner,panel,title,JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE)==JOptionPane.OK_OPTION;
    }
    void info(String text){JOptionPane.showMessageDialog(owner,text);}
    static String required(JTextField field) {
        String s=field.getText().strip();
        if(s.isEmpty()) throw new IllegalArgumentException("Complete the required fields.");
        return s;
    }
    static List<Object[]> read(String sql,Object... args) throws SQLException {
        try(var c=DBConnection.getConnection()){return LibraryStore.rows(c,sql,args);}
    }
    JComboBox<LibraryStore.Choice> choices(String sql) throws SQLException {
        var box=new JComboBox<LibraryStore.Choice>();
        for(Object[] r:read(sql)) box.addItem(new LibraryStore.Choice(r[0].toString(),r[1]+" ("+r[0]+")"));
        box.setPrototypeDisplayValue(new LibraryStore.Choice("","Select a book or member from this list"));
        return box;
    }
    static String selected(JComboBox<LibraryStore.Choice> box){
        var item=(LibraryStore.Choice)box.getSelectedItem();
        if(item==null) throw new IllegalArgumentException("No eligible records to select.");
        return item.id();
    }
    void book(String existing) throws SQLException {
        JTextField id=new JTextField(),title=new JTextField(),author=new JTextField(),category=new JTextField();
        if(existing!=null){
            var row=read("SELECT title,author,category FROM books WHERE book_id=?",existing).get(0);
            id.setText(existing);id.setEditable(false);title.setText((String)row[0]);author.setText((String)row[1]);category.setText((String)row[2]);
        }
        if(!form(existing==null?"Add book":"Edit book","Book ID",id,"Title",title,"Author",author,"Category",category))return;
        try(var c=DBConnection.getConnection()){
            if(existing==null) LibraryStore.update(c,"INSERT INTO books(book_id,title,author,category,is_issued) VALUES(?,?,?,?,0)",required(id),required(title),required(author),category.getText().strip());
            else LibraryStore.update(c,"UPDATE books SET title=?,author=?,category=? WHERE book_id=?",required(title),required(author),category.getText().strip(),existing);
        }
    }
    void member(String existing) throws SQLException {
        JTextField name=new JTextField(),email=new JTextField();
        if(existing!=null){
            var r=read("SELECT name,email FROM members WHERE member_id=?",existing).get(0);
            name.setText((String)r[0]);email.setText((String)r[1]);
            if(form("Edit member "+existing,"Name",name,"Email",email))
                try(var c=DBConnection.getConnection()){LibraryStore.update(c,"UPDATE members SET name=?,email=? WHERE member_id=?",required(name),email.getText().strip(),existing);}
        } else {
            JPasswordField pw=new JPasswordField(18);
            if(!form("Register member","Name",name,"Email",email,"Login password",pw))return;
            String pass=new String(pw.getPassword());
            if(pass.isBlank())throw new IllegalArgumentException("Enter a login password.");
            info("Member created. Library ID: "+LibraryStore.register(required(name),email.getText().strip(),pass));
        }
    }
    void remove(boolean book,String id) throws SQLException{
        if(JOptionPane.showConfirmDialog(owner,"Delete "+id+"?","Confirm deletion",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        try(var c=DBConnection.getConnection()){
            String sql=book?"DELETE FROM books WHERE book_id=? AND is_issued=0":"DELETE FROM members WHERE member_id=? AND COALESCE(fine_due,0)=0";
            try(var p=LibraryStore.prepare(c,sql,id)){
                if(p.executeUpdate()!=1)throw new SQLException("This record has an active loan or dues, or has already been removed.");
            }catch(SQLIntegrityConstraintViolationException ex){throw new SQLException("Keep this record because it has borrowing or payment history.");}
        }
    }
    void issue() throws SQLException {
        var members=choices("SELECT member_id,name FROM members ORDER BY name");
        var books=choices("SELECT book_id,title FROM books WHERE is_issued=0 ORDER BY title");
        LocalDate today = LocalDate.parse(read("SELECT CURRENT_DATE").get(0)[0].toString());
        JTextField issued = new JTextField(today.toString());
        JTextField due = new JTextField(today.plusDays(14).toString());
        if (form("Issue book", "Member", members, "Available copy", books,
                "Issue date (YYYY-MM-DD)", issued, "Due date (YYYY-MM-DD)", due)) {
            LibraryStore.issue(selected(members), selected(books), date(issued), date(due));
            info("Book issued. Due on " + due.getText().strip());
        }
    }

    private static LocalDate date(JTextField field) {
        try {
            return LocalDate.parse(field.getText().strip());
        } catch (java.time.format.DateTimeParseException ex) {
            throw new IllegalArgumentException("Enter a valid date in YYYY-MM-DD format.");
        }
    }

    void editDueDate(String id) throws SQLException {
        var rows = read("SELECT issue_date,due_date FROM transactions WHERE transaction_id=? AND return_date IS NULL", id);
        if (rows.isEmpty()) throw new SQLException("This loan is already closed.");
        Object[] loan = rows.get(0);
        LocalDate original = LocalDate.parse(loan[1].toString());
        JTextField due = new JTextField(original.toString());
        if (form("Edit due date · Transaction " + id, "Issue date", new JLabel(loan[0].toString()),
                "Due date (YYYY-MM-DD)", due)) {
            LibraryStore.changeDueDate(id, original, date(due));
        }
    }
    void giveBack(String id) throws SQLException {
        var r=read("SELECT b.title,m.name,t.due_date,2*GREATEST(0,DATEDIFF(CURRENT_DATE,t.due_date)) FROM transactions t JOIN books b USING(book_id) JOIN members m USING(member_id) WHERE t.transaction_id=? AND return_date IS NULL",id);
        if(r.isEmpty())throw new SQLException("This loan is already closed.");
        Object[] a=r.get(0);
        if(form("Return transaction "+id,"Book",new JLabel(a[0].toString()),"Member",new JLabel(a[1].toString()),"Due date",new JLabel(a[2].toString()),"Fine today",new JLabel("₹"+a[3])))
            info("Return recorded. Fine charged: ₹"+LibraryStore.returnBook(id, LocalDate.parse(a[2].toString())));
    }
    void collect(String member,boolean demo) throws SQLException {
        JComboBox<LibraryStore.Choice> due=new JComboBox<>();
        try(var c=DBConnection.getConnection()){for(var d:LibraryStore.dues(c,member))due.addItem(d);}
        if(due.getItemCount()==0){info("No outstanding charges to settle.");return;}
        var method=new JComboBox<>(demo?new String[]{"UPI","Card"}:new String[]{"Cash","UPI","Card"});
        Font paymentFont=new Font("Segoe UI",Font.PLAIN,17);
        due.setFont(paymentFont);method.setFont(paymentFont);
        int width=Math.max(300,due.getPreferredSize().width+24);
        due.setPreferredSize(new Dimension(width,38));
        method.setPreferredSize(new Dimension(width,38));
        if(form(demo?"Simulate payment (semester demo)":"Record received payment","Charge",due,"Method",method)){
            String mode=(String)method.getSelectedItem();
            info(LibraryStore.pay(member,(LibraryStore.Choice)due.getSelectedItem(),demo?"Demo "+mode:mode));
        }
    }
    static JTable grid(String[] headers,List<Object[]> rows){
        JTable t=new JTable(new DefaultTableModel(rows.toArray(Object[][]::new),headers){
            public boolean isCellEditable(int r,int c){return false;}
        });
        t.setAutoCreateRowSorter(true);t.setRowHeight(30);
        return t;
    }
    static void export(Component owner,JTable t) throws Exception {
        JFileChooser fc=new JFileChooser();
        fc.setSelectedFile(new java.io.File("library-report.csv"));
        if(fc.showSaveDialog(owner)!=JFileChooser.APPROVE_OPTION)return;
        var path=fc.getSelectedFile().toPath();
        if(Files.exists(path)&&JOptionPane.showConfirmDialog(owner,"Replace "+path.getFileName()+"?","Export CSV",JOptionPane.YES_NO_OPTION)!=JOptionPane.YES_OPTION)return;
        StringBuilder out=new StringBuilder();
        for(int i=-1;i<t.getRowCount();i++){
            for(int j=0;j<t.getColumnCount();j++){
                if(j>0)out.append(',');
                Object v=i<0?t.getColumnName(j):t.getValueAt(i,j);
                out.append('"').append(v==null?"":v.toString().replace("\"","\"\"")).append('"');
            }
            out.append(System.lineSeparator());
        }
        Files.writeString(path,out);
    }
    void report(String title,String[] headings,List<Object[]> rows,String summary){
        var t=grid(headings,rows);
        var panel=new JPanel(new BorderLayout(8,8));panel.add(new JScrollPane(t));
        panel.setPreferredSize(new Dimension(760,340));panel.add(new JLabel(summary),BorderLayout.SOUTH);
        Object[] opts={"Close","Export CSV","Print / PDF"};
        int pick=JOptionPane.showOptionDialog(owner,panel,title,JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,opts,opts[0]);
        try{if(pick==1)export(owner,t);if(pick==2)t.print(JTable.PrintMode.FIT_WIDTH,new java.text.MessageFormat(title),null);}
        catch(Exception e){info(e.getMessage());}
    }
    void daily() throws SQLException {
        JTextField date=new JTextField(LocalDate.now().toString());
        if(!form("Daily fine collection","Payment date (YYYY-MM-DD)",date))return;
        var day=LocalDate.parse(required(date));
        var data=read("SELECT p.receipt_id,m.name,p.transaction_id,p.paid_at,p.method,p.amount FROM library_payments p JOIN members m USING(member_id) WHERE p.paid_at>=? AND p.paid_at<? ORDER BY p.receipt_id",day.toString(),day.plusDays(1).toString());
        var totals=new java.util.LinkedHashMap<String,java.math.BigDecimal>();
        var sum=java.math.BigDecimal.ZERO;
        for(var r:data){var amount=(java.math.BigDecimal)r[5];String method=r[4].toString();totals.merge(method,amount,java.math.BigDecimal::add);if(!method.startsWith("Demo "))sum=sum.add(amount);}
        report("Collections · "+day,new String[]{"Receipt","Member","Transaction ID","Paid at","Method","Amount (₹)"},data,
            totals.entrySet().stream().map(e->e.getKey()+" ₹"+e.getValue()).collect(Collectors.joining("   "))+"     Desk collections ₹"+sum);
    }
}
