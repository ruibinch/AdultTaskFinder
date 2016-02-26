package storage;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.DirectoryNotEmptyException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class FilePath {

        private static final String SAVE_FILE_NAME = "saveInfo.txt";
        private static final String DATA_FILE_NAME = "data.csv";
    
        /**
         * Changes the default directory location to store the data file to the provided path.
         * Path provided MUST be valid.
         * <p>
         * @param saveDir Location of new directory to contain data file for saved tasks
         */
        static void changeDirectory(Path saveDir) {
            //boolean isValidDir = checkValidDir();
            
            FileWriter fileWriter = null;
            try {
                fileWriter = new FileWriter(SAVE_FILE_NAME , false);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            PrintWriter printWriter = new PrintWriter(fileWriter);
            printWriter.print(saveDir.toString());
            printWriter.close();
        }
        
        /**
         * Returns the path of the file containing saved tasks. If no path is defined, 
         * sets the default save location to the working directory and returns the path
         * of the file to be created in the working directory.
         * <p>
         * @return String of path of the file containing saved tasks
         */
        static String getPath() {
            Path saveDir = Paths.get(".");
            try {
                BufferedReader fileReader = new BufferedReader(new FileReader(SAVE_FILE_NAME));
                saveDir = Paths.get(fileReader.readLine());
                fileReader.close();
            } catch (IOException e) {
                changeDirectory(saveDir);
                Path filePath = Paths.get(".", DATA_FILE_NAME);
                return filePath.toString();
            }
            Path filePath = Paths.get(saveDir.toString(), DATA_FILE_NAME);
            return filePath.toString();
        }
        
        static void deleteData() {
            Path path = Paths.get(".", DATA_FILE_NAME);
            try {
                Files.delete(path);
            } catch (NoSuchFileException x) {
                System.err.format("%s: no such" + " file or directory%n", path);
            } catch (DirectoryNotEmptyException x) {
                System.err.format("%s not empty%n", path);
            } catch (IOException x) {
                // File permission problems are caught here.
                System.err.println(x);
            }
        }

        static String setPath(String filePath, String fileName) {
            return Paths.get(filePath, fileName).toString();
        }
        
}
