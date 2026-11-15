package storage;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;

import core.Database;

// сохранение бд в файл и загрузки бд из файла
public class StorageManager {
 
    public static void save(Database database, String file) throws IOException{
        FileOutputStream new_file = new FileOutputStream(file);
        ObjectOutputStream output = new ObjectOutputStream(new_file);
        output.writeObject(database);
        output.close();
    }

    public static Database load(String file) throws IOException, ClassNotFoundException{
        FileInputStream new_file = new FileInputStream(file);
        ObjectInputStream restored_db = new ObjectInputStream(new_file);
        Database database = (Database) restored_db.readObject();
        restored_db.close();
        return database;
    }
    
}
