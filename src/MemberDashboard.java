import java.awt.*;
import java.awt.print.PrinterException;
import java.sql.SQLException;
import java.util.List;
import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.table.TableRowSorter;
import javax.swing.event.*;
import java.util.regex.Pattern;

public class MemberDashboard extends JFrame {
    private final CurrentUser user;
    private final LibraryDesk desk=new LibraryDesk(this);
    private int active;
    private final JPanel body = new JPanel(new BorderLayout());
    private final JLabel heading = text("",26,true);
    private final JLabel count = text("",14,false);
    private final JButton[] nav = new JButton[3];
    private final JLabel[] figures = new JLabel[4];
    private final JButton receipt = button("View receipt", DashboardStyle.BLUE, Color.WHITE);
    private final JButton pay = button("Demo payment", new Color(0x087760), Color.WHITE);

    private final JTextField search=new JTextField(16);
    private final String[] names={"My borrowing","Catalogue","Payment history"};
    private final String[][] columns={
        {"Transaction ID","Book","Issue date","Due date","Return date","Status","Fine (₹)"},
        {"Book ID","Title","Author","Category","Availability"},
        {"Receipt","Transaction ID","Paid at","Method","Amount (₹)"}
    };
    private final JTable[] tables=new JTable[3];
    public MemberDashboard(CurrentUser user){
        super("Member Dashboard");
        this.user=user;
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        JPanel root = new JPanel(new BorderLayout());
        JPanel side = new JPanel(new BorderLayout());
        side.setBackground(DashboardStyle.NAVY);
        side.setBorder(new EmptyBorder(18,14,16,14));
        side.setPreferredSize(new Dimension(195,0));
        JPanel links = new JPanel(new GridLayout(0,1,0,7));
        links.setOpaque(false);
        JLabel brand = text("Digital Library",20,true);
        brand.setForeground(Color.WHITE);
        links.add(brand);
        JLabel campus = text("NIET Greater Noida",14,false);
        campus.setForeground(new Color(0xBDCADC));
        links.add(campus);
        for(int i=0;i<names.length;i++){
            final int target=i;
            nav[i]=button(names[i],DashboardStyle.NAVY,Color.WHITE);
            nav[i].setHorizontalAlignment(SwingConstants.LEFT);
            nav[i].addActionListener(e->select(target));
            links.add(nav[i]);
        }
        side.add(links,BorderLayout.NORTH);
        JButton logout=button("Log out",new Color(0x903244),Color.WHITE);
        logout.addActionListener(e->{dispose();new LoginFrame().setVisible(true);});
        side.add(logout,BorderLayout.SOUTH);
        root.add(side,BorderLayout.WEST);

        JPanel page=new JPanel(new BorderLayout(0,18));
        page.setBackground(new Color(0xF5F7FB));
        page.setBorder(new EmptyBorder(22,22,22,22));
        JPanel top=new JPanel(new BorderLayout(12,18));
        top.setOpaque(false);
        JPanel identity=new JPanel(new BorderLayout(0,3));
        identity.setOpaque(false);
        identity.add(text("Member dashboard",27,true),BorderLayout.NORTH);
        identity.add(text(user.name+"   ·   "+user.id,15,false));
        top.add(identity,BorderLayout.NORTH);
        JPanel cards=new JPanel(new GridLayout(1,4,12,0));
        cards.setOpaque(false);
        String[] labels={"Books borrowed","Books returned","Overdue books","Outstanding dues"};
        for(int i=0;i<4;i++){
            JPanel card=DashboardStyle.card(14);
            card.add(text(labels[i],14,false),BorderLayout.NORTH);
            figures[i]=text("—",26,true);
            figures[i].setForeground(DashboardStyle.ACCENTS[i]);
            card.add(figures[i]);
            cards.add(card);
        }
        top.add(cards);
        page.add(top,BorderLayout.NORTH);
        JPanel card=DashboardStyle.card(16);
        JPanel toolbar=new JPanel(new BorderLayout(10,8));
        toolbar.setOpaque(false);
        toolbar.add(heading,BorderLayout.WEST);
        JPanel tools=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        tools.setOpaque(false);
        tools.add(text("Search",14,false));tools.add(search);
        JButton refresh=button("Refresh",new Color(0xE7EFFB),DashboardStyle.BLUE);
        tools.add(refresh);
        toolbar.add(tools,BorderLayout.EAST);
        card.add(toolbar,BorderLayout.NORTH);
        card.add(body);
        JPanel footer=new JPanel(new BorderLayout());
        footer.setOpaque(false);
        footer.add(count,BorderLayout.WEST);
        JPanel actions=new JPanel(new FlowLayout(FlowLayout.RIGHT,6,0));
        actions.setOpaque(false);
        actions.add(pay);actions.add(receipt);
        footer.add(actions,BorderLayout.EAST);
        card.add(footer,BorderLayout.SOUTH);
        page.add(card);root.add(page);setContentPane(root);
        pay.addActionListener(e->{try{desk.collect(user.id,true);load();}catch(Exception ex){desk.info(ex.getMessage());}});
        receipt.addActionListener(e->showReceipt());
        refresh.addActionListener(e->load());
        search.setFont(new Font("Segoe UI",Font.PLAIN,15));
        search.getDocument().addDocumentListener(new DocumentListener(){
            public void insertUpdate(DocumentEvent e){filter();}
            public void removeUpdate(DocumentEvent e){filter();}
            public void changedUpdate(DocumentEvent e){filter();}
        });
        setSize(1140,655);setMinimumSize(new Dimension(850,480));setLocationRelativeTo(null);
        select(0);load();
    }
    private static JLabel text(String s,int size,boolean bold){
        JLabel label=new JLabel(s);
        label.setFont(new Font("Segoe UI",bold?Font.BOLD:Font.PLAIN,size));
        label.setForeground(new Color(0x172B42));return label;
    }
    private static JButton button(String s,Color bg,Color fg){
        JButton b=new JButton(s);
        b.setUI(new javax.swing.plaf.basic.BasicButtonUI());
        b.setFont(new Font("Segoe UI",Font.BOLD,15));
        b.setBorder(new EmptyBorder(9,13,9,13));b.setOpaque(true);
        b.setBackground(bg);b.setForeground(fg);return b;
    }
    private void select(int index){
        active=index;heading.setText(names[index]);search.setText("");
        for(int i=0;i<nav.length;i++) nav[i].setBackground(i==active?DashboardStyle.BLUE:DashboardStyle.NAVY);
        receipt.setVisible(index==2);pay.setVisible(index!=1);display();
    }
    private void display(){
        body.removeAll();
        if(tables[active]!=null){
            JScrollPane scroll=new JScrollPane(tables[active]);
            scroll.setBorder(BorderFactory.createEmptyBorder());body.add(scroll);
            filter();
        }
        body.revalidate();body.repaint();
    }
    private void load(){
        try{
            put(0,LibraryDesk.read("SELECT t.transaction_id,b.title,t.issue_date,t.due_date,t.return_date,CASE WHEN t.return_date IS NOT NULL THEN 'Returned' WHEN t.due_date<CURRENT_DATE THEN 'Overdue' ELSE 'Issued' END,CASE WHEN t.return_date IS NULL THEN 2*GREATEST(0,DATEDIFF(CURRENT_DATE,t.due_date)) ELSE t.fine_amount END FROM transactions t JOIN books b USING(book_id) WHERE member_id=? ORDER BY t.transaction_id DESC",user.id));
            put(1,LibraryDesk.read("SELECT book_id,title,author,category,CASE WHEN is_issued THEN 'Issued' ELSE 'Available' END FROM books ORDER BY title"));
            put(2,LibraryDesk.read("SELECT receipt_id,transaction_id,paid_at,method,amount FROM library_payments WHERE member_id=? ORDER BY receipt_id DESC",user.id));
            var b=LibraryDesk.read("SELECT COALESCE(fine_due,0) FROM members WHERE member_id=?",user.id);
            int borrowed=0,returned=0,overdue=0;
            var model=tables[0].getModel();
            for(int i=0;i<model.getRowCount();i++){
                Object state=model.getValueAt(i,5);
                if("Returned".equals(state)) returned++; else borrowed++;
                if("Overdue".equals(state)) overdue++;
            }
            figures[0].setText(String.valueOf(borrowed));
            figures[1].setText(String.valueOf(returned));
            figures[2].setText(String.valueOf(overdue));
            figures[3].setText("₹"+b.get(0)[0]);
            display();
        }catch(SQLException ex){desk.info(ex.getMessage());}
    }
    private void put(int index,List<Object[]> data){
        tables[index]=LibraryDesk.grid(columns[index],data);
        JTable t=tables[index];
        t.setFont(new Font("Segoe UI",Font.PLAIN,15));t.setRowHeight(39);
        t.setShowVerticalLines(false);t.setGridColor(new Color(0xDFE6EF));
        t.setSelectionBackground(new Color(0xDCEAFF));t.setFillsViewportHeight(true);
        t.getTableHeader().setFont(new Font("Segoe UI",Font.BOLD,14));
        t.getTableHeader().setReorderingAllowed(false);
        t.getTableHeader().setPreferredSize(new Dimension(0,39));
        if(index<2) t.getColumnModel().getColumn(index==0?5:4).setCellRenderer(new DashboardStyle.StatusCell());
        if(index==0)t.getColumnModel().getColumn(0).setMinWidth(110);
    }
    private void filter(){
        int i=active;
        if(i>=0 && tables[i]!=null && tables[i].getRowSorter() instanceof TableRowSorter<?> sorter)
            sorter.setRowFilter(RowFilter.regexFilter("(?iu)"+Pattern.quote(search.getText().strip())));
        if(tables[i]!=null)count.setText(tables[i].getRowCount()==0?"No matching records":tables[i].getRowCount()+" records");
    }
    private void showReceipt(){
        JTable t=tables[2];
        if(t==null || t.getSelectedRow()<0){desk.info("Select a payment first.");return;}
        int r=t.getSelectedRow();
        StringBuilder s=new StringBuilder("NIET Digital Library\nPayment receipt\n\nMember: "+user.name+" ("+user.id+")\n");
        for(int i=0;i<t.getColumnCount();i++){
            Object v=t.getValueAt(r,i);
            s.append(t.getColumnName(i)).append(": ").append(v==null?"Previous balance":v).append('\n');
        }
        JTextArea area=new JTextArea(s.toString());area.setEditable(false);
        int choice=JOptionPane.showOptionDialog(this,area,"Payment receipt",JOptionPane.DEFAULT_OPTION,JOptionPane.PLAIN_MESSAGE,null,new String[]{"Close","Print / PDF"},"Close");
        if(choice==1)try{area.print();}catch(PrinterException ex){desk.info(ex.getMessage());}
    }
}
