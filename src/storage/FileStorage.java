//@@author A0080510X

package storage;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import com.google.gson.JsonSyntaxException;

import common.TaskObject;

/**
 * Implementation of the APIs of the Storage component.
 * @author Hang
 *
 */
public class FileStorage implements IStorage {

    private static FileStorage instance = null;

    private FileStorage() {
    }

    public static FileStorage getInstance() {
        if (instance == null) {
            instance = new FileStorage();
        }
        return instance;
    }

    @Override
    public void save(ArrayList<TaskObject> newTaskList) 
            throws NoSuchFileException, IOException {
        overwriteSave(newTaskList);
        overwriteBackup(newTaskList);
    }

    @Override
    public ArrayList<TaskObject> load() 
            throws InvalidPathException, FileNotFoundException, IOException , JsonSyntaxException {
        String filePath = null;
        try {
            filePath = FilePath.getPath();
        } catch (FileNotFoundException e) {
            return new ArrayList<TaskObject>();
        } 
        ArrayList<TaskObject> taskList = load(filePath);
        return taskList;
    }
    
    @Override
    public ArrayList<TaskObject> load(String filePath) 
            throws InvalidPathException, FileNotFoundException, IOException, JsonSyntaxException{
        if (!FilePath.pathValid(filePath)) {
            throw new InvalidPathException(filePath, "Invalid Directory");
        }
        ArrayList<TaskObject> taskList = TaskData.readTasks(filePath);
        return taskList;
    }
    
    @Override
    public ArrayList<TaskObject> loadBackup() throws InvalidPathException, 
            JsonSyntaxException, FileNotFoundException, IOException {
        return load(Constants.FILEPATH_BACKUP_DATA.toString());
    }
    
    @Override
    public String createCopy(String directory , String fileName) 
            throws InvalidPathException ,IOException  {
        if (!FilePath.directoryValid(directory)) {
            throw new InvalidPathException(directory, "Invalid Directory");
        }
        ArrayList<TaskObject> taskList = load();
        return writeNewTxtFile(directory, fileName, taskList);
    }

    private String writeNewTxtFile(String directory, String fileName, ArrayList<TaskObject> taskList) 
            throws IOException {
        String filePath = Paths.get(directory, fileName).toString();
        File toSave = new File(filePath.concat(".txt"));
        String suffix = "(%d)";
        for (int i = 1; toSave.exists(); i++ ) {
            filePath = Paths.get(directory, fileName.concat(String.format(suffix, i)))
                    .toString();
            toSave = new File(filePath);
        }
        filePath = filePath.concat(".txt");
        TaskData.writeTasks(taskList, filePath);
        return filePath;
    }

    @Override
    public void changeSaveLocation (String directory) 
            throws InvalidPathException, IOException {
        if (!FilePath.directoryValid(directory)) {
            throw new InvalidPathException(directory, "Invalid Directory");
        }
        ArrayList<TaskObject> taskList = load();
        try {
            deleteExistingSave();
        } catch (FileNotFoundException e) {
            //No existing Saved File to delete
        } 
        FilePath.changePreferedDirectory(directory);
        save(taskList);
    }
    
    private void overwriteBackup(ArrayList<TaskObject> newTaskList) throws IOException {
        String filePath = Constants.FILEPATH_BACKUP_DATA.toString();
        TaskData.writeTasks(newTaskList, filePath);
    }

    private void overwriteSave(ArrayList<TaskObject> newTaskList) throws IOException, FileNotFoundException {
        String filePath = null;
        try {
        filePath = FilePath.getPath();
        } catch (FileNotFoundException e) {
            FilePath.initializeDefaultSave();
            filePath = FilePath.getPath();
        }
        if (filePath == null) { // defensive measure
            FilePath.initializeDefaultSave();
            filePath = FilePath.getPath();
        }
        TaskData.writeTasks(newTaskList, filePath);
    }
    
    private void deleteExistingSave() throws FileNotFoundException, IOException {
        String filePath = FilePath.getPath();
        Path path = Paths.get(filePath);
        Files.deleteIfExists(path);
    }
    
}
