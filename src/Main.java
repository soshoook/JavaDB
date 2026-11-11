import core.*;
import storage.StorageManager;

import java.io.IOException;
import java.time.LocalDate;
import java.io.File;

public class Main {

    public static void main(String[] args) throws IOException, ClassNotFoundException {
        File file = new File("database.dat");
        Database database;
        if (file.exists()){
            database = StorageManager.load("database.dat");
            System.out.println("Database was loaded!");
        }
        else {
            database = new Database();
            System.out.println("Database was created!");
        }

        // клубы
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

        // мероприятия
        Table events = new Table("events");
        events.addColumn(new Column("id", DataType.INTEGER, true));
        events.addColumn(new Column("department_id", DataType.INTEGER, false));
        events.addColumn(new Column("name", DataType.STRING, false));
        events.addColumn(new Column("ticket_price", DataType.DOUBLE, false));
        events.addColumn(new Column("event_date", DataType.DATE, false));
        events.addColumn(new Column("active", DataType.BOOLEAN, false));

        Row event1 = new Row();
        event1.setValue("id", 1);
        event1.setValue("department_id", 1);
        event1.setValue("name", "Java Conference");
        event1.setValue("ticket_price", 1000.0);
        event1.setValue("event_date", LocalDate.of(2026, 11, 15));
        event1.setValue("active", true);
        events.addRow(event1);

        Row event3 = new Row();
        event3.setValue("id", 3);
        event3.setValue("department_id", 2);
        event3.setValue("name", "Art Exhibition");
        event3.setValue("ticket_price", 300.0);
        event3.setValue("event_date", LocalDate.of(2026, 11, 25));
        event3.setValue("active", true);
        events.addRow(event3);
        database.addTable(events);

        StorageManager.save(database, file.getPath());

        // итоговая таблица
        Table res = database.getTable("clubs");
        System.out.println("Table: " + res.getName());
        for (Row row : res.getRow()) {
            System.out.println(
                    row.getValue("id") + " | " +
                            row.getValue("name") + " | " +
                            row.getValue("budget") + " | " +
                            row.getValue("created_at") + " | " +
                            row.getValue("active")
            );
        }

        Table res1 = database.getTable("events");
        System.out.println("\nTable: " + res1.getName());
        for (Row row : res1.getRow()) {
            System.out.println(
                    row.getValue("id") + " | " +
                            row.getValue("department_id") + " | " +
                            row.getValue("name") + " | " +
                            row.getValue("ticket_price") + " | " +
                            row.getValue("event_date") + " | " +
                            row.getValue("active")
            );
        }

    }
}