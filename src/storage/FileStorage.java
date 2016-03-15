package storage;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;

import common.TaskObject;

public class FileStorage implements Storage {

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
    public  void save(ArrayList<TaskObject> newTaskList) throws IOException {
        String filePath = null;
        filePath = FilePath.getPath();
        Path path = Paths.get(filePath);
        Files.deleteIfExists(path);
        TaskData.writeTasks(newTaskList, filePath);
    }

    @Override
    public ArrayList<TaskObject> load() throws IOException  {
       
        String filePath;
        ArrayList<TaskObject> taskList = null;
        try {
            filePath = FilePath.getPath();
            taskList = new ArrayList<TaskObject>(TaskData.readTasks(filePath));
        } catch (NoSuchFileException e) {
            FilePath.prepareDefaultSave();
        } 
        return taskList;
    }

    @Override
    public void createCopy(String directory , String fileName) throws InvalidPathException ,IOException  {
        if (!FilePath.checkPath(directory)) {
            throw new InvalidPathException(directory, "Invalid Directory");
        }
        ArrayList<TaskObject> taskList = load();
        String copyFilePath = Paths.get(directory, fileName).toString();
        TaskData.writeTasks(taskList, copyFilePath);
    }

    @Override
    public void changeSaveLocation (String directory) throws InvalidPathException , IOException {
        if (!FilePath.checkPath(directory)) {
            throw new InvalidPathException(directory, "Invalid Directory");
        }
        ArrayList<TaskObject> taskList = load();
        String filePath = FilePath.getPath();
        Path path = Paths.get(filePath);
        Files.deleteIfExists(path);
        FilePath.changeDirectory(directory);
        save(taskList);
    }

    public ArrayList<TaskObject> load(String directory, String fileName) throws InvalidPathException , IOException{
        if (!FilePath.checkPath(directory)) {
            throw new InvalidPathException(directory, "Invalid Directory");
        }
        String filePath = Paths.get(directory, fileName).toString();
        ArrayList<TaskObject> taskList = TaskData.readTasks(filePath);
        return taskList;
    }

}
