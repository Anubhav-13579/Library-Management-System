# NIET Digital Library Management System

Java 17 Swing application with librarian and member dashboards, lending, overdue fines, receipts and daily collection reports.

## Run in Windows PowerShell

1. Place your MySQL Connector/J JAR in a folder named `lib` at the repository root.
2. For a fresh database run `sql/01_schema.sql` followed by `sql/02_payments.sql` in MySQL. For the existing project database use your existing accounts. A new database needs a librarian account provisioned locally before first login.
3. Set `$env:LIBRARY_DB_USER` and, if required, `$env:LIBRARY_DB_PASSWORD` to your local MySQL credentials in the terminal. The default URL uses localhost:3306/lib_sys_db.
4. From the repository root run:

```powershell
cd src
javac -encoding UTF-8 -cp ".;..\lib\*" *.java
if ($?) { java -cp ".;..\lib\*" Main }
```

Member payments are simulated. The demo schema currently uses plaintext application passwords.
