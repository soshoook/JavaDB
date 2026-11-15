import core.*;
import storage.StorageManager;
import sql.SQLExecutor;

import java.io.File;
import java.io.IOException;
import java.io.InvalidClassException;
import java.util.Scanner;

// точка входа в программу
// загрузка бд/создание новой, инициализация экзекьютора и взаимодействие с юзером через консоль
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
            database = new Database();
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
}
