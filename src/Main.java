import core.*;
import storage.StorageManager;
import sql.SQLExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.time.LocalDate;
import java.util.Scanner;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File file = new File("database.dat");
        Database database;

        if (file.exists()) {
            try {
                database = StorageManager.load(file.getPath());
            } catch (InvalidClassException e) {
                System.out.println("database.dat was saved in an old format. Delete it and run the program again.");
                return;
            }
            System.out.println("Database was loaded!");
        } else {
            database = createDefaultDatabase();
            StorageManager.save(database, file.getPath());
            System.out.println("Database was created!");
        }

        SQLExecutor executor = new SQLExecutor(database);
        Scanner scanner = new Scanner(System.in);

        System.out.println("Enter SQL commands. Type exit to stop.");

        while (true) {
            System.out.print("> ");
            if (!scanner.hasNextLine())
                break;

            String query = scanner.nextLine().trim();

            if (query.equalsIgnoreCase("exit"))
                break;

            if (query.isEmpty())
                continue;

            try {
                executor.execute(query);

                if (!query.toUpperCase().startsWith("SELECT")) {
                    StorageManager.save(database, file.getPath());
                }
            } catch (IllegalArgumentException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (Exception e) {
                System.out.println("Error: " + e);
            }
        }

        scanner.close();
        StorageManager.save(database, file.getPath());
        System.out.println("Database saved!");
    }

    private static Database createDefaultDatabase() {
        Database database = new Database();

        Table clubs = new Table("clubs");
        clubs.addColumn(new Column("id", DataType.INTEGER, true));
        clubs.addColumn(new Column("name", DataType.STRING, false));
        clubs.addColumn(new Column("budget", DataType.DOUBLE, false));
        clubs.addColumn(new Column("created_at", DataType.DATE, false));
        clubs.addColumn(new Column("active", DataType.BOOLEAN, false));

        Row club1 = new Row();
        club1.setValue("id", 1);
        club1.setValue("name", "IT Club");
        club1.setValue("budget", 50000.0);
        club1.setValue("created_at", LocalDate.of(2026, 11, 10));
        club1.setValue("active", true);
        clubs.addRow(club1);

        Row club2 = new Row();
        club2.setValue("id", 2);
        club2.setValue("name", "Art Club");
        club2.setValue("budget", 30000.0);
        club2.setValue("created_at", LocalDate.of(2026, 11, 9));
        club2.setValue("active", true);
        clubs.addRow(club2);

        database.addTable(clubs);

        Table events = new Table("events");
        events.addColumn(new Column("id", DataType.INTEGER, true));
        events.addColumn(new Column("department_id", DataType.INTEGER, false));
        events.addColumn(new Column("name", DataType.STRING, false));
        events.addColumn(new Column("ticket_price", DataType.DOUBLE, false));
        events.addColumn(new Column("event_date", DataType.DATE, false));
        events.addColumn(new Column("active", DataType.BOOLEAN, false));

        events.addForeignKey(new ForeignKey("department_id", "clubs", "id"));

        database.addTable(events);

        Row event1 = new Row();
        event1.setValue("id", 1);
        event1.setValue("department_id", 1);
        event1.setValue("name", "Java Conference");
        event1.setValue("ticket_price", 1000.0);
        event1.setValue("event_date", LocalDate.of(2026, 11, 15));
        event1.setValue("active", true);
        database.addRow("events", event1);

        Row event2 = new Row();
        event2.setValue("id", 2);
        event2.setValue("department_id", 2);
        event2.setValue("name", "Art Exhibition");
        event2.setValue("ticket_price", 300.0);
        event2.setValue("event_date", LocalDate.of(2026, 11, 25));
        event2.setValue("active", true);
        database.addRow("events", event2);

        return database;
    }
}
