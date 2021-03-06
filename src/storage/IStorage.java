//@@author A0080510X

package storage;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.InvalidPathException;
import java.util.ArrayList;

import com.google.gson.JsonSyntaxException;

import common.TaskObject;

/**
 * Interface Defining all the APIs for the storage component.
 * @author Hang
 *
 */
public interface IStorage {

    /**
     * Writes tasks to storage. Overwrites existing tasks stored in storage.
     * If no existing preferred save location is found, the preferred save location
     * will be set to the default save location. 
     * <p>
     * @param taskList - The list of tasksObjects to be written
     * @throws IOException Error with saving tasks to disks
     */
    void save(ArrayList<TaskObject> taskList) throws IOException;

    /**
     * Loads all saved tasks into storage from existing specified file. If no existing 
     * existing preferred save location is specified, returns an empty list.
     * @return List of TaskObjects. Empty list returned if no save location is specified.
     * @throws FileNotFoundException Unable to find file in specified save location.
     * @throws IOException Error reading from disk.
     * @throws JsonSyntaxException File not in correct format.
     */
    ArrayList<TaskObject> load() throws FileNotFoundException, IOException , JsonSyntaxException;
    
    /**
     * Creates a copy of the file with the specified file name containing all stored task 
     * information at the specified directory. If existing files exist at the specified path,
     * the file copy will be saved with a suffix. The file will not be saved with any extension.
     * <p>
     * The tasks stored in storage should first be updated using <code>load</code> or <code>save</code>
     * for the created copy to contain the most recent task information.
     * <p>
     * @param directory Path of directory to create the copy in
     * @param fileName Name of file to be created
     * @return The file path of the created file copy.
     * @throws IOException Error Copying Existing Files
     * @throws InvalidPathException  The path specified cannot be used
     */
    String createCopy(String directory, String fileName) throws InvalidPathException, IOException;

    /**
     * <p>
     * @param directory The new preferred directory to store the task data file.
     * @throws IOException Error writing to specified directory
     * @throws InvalidPathException The specified directory cannot be used
     */
    void changeSaveLocation(String directory) throws InvalidPathException, IOException;

    /**
     * Load from specified path.
     * 
     * @param filePath The filePath of the data file to be read.
     * @return The list of tasks read from the data file.
     * @throws InvalidPathException The specified path is invalid.
     * @throws IOException Error reading from file.
     * @throws FileNotFoundException No file found at the specified path.
     * @throws JsonSyntaxException Specified file is not in correct format.
     */
    ArrayList<TaskObject> load(String filePath)
            throws InvalidPathException, IOException, FileNotFoundException, JsonSyntaxException;
    
    /**
     * Load from backup file.
     * @return The list of tasks read from the backup file.
     * @throws InvalidPathException The specified path is invalid.
     * @throws IOException Error reading from file.
     * @throws FileNotFoundException No file found at the specified path.
     * @throws JsonSyntaxException Specified file is not in correct format.
     */
    ArrayList<TaskObject> loadBackup() throws InvalidPathException, JsonSyntaxException, 
            FileNotFoundException, IOException;
    
}
