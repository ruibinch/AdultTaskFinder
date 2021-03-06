//@@author A0124052X

package logic.mark;

import logic.exceptions.MarkException;
import storage.FileStorage;
import storage.IStorage;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.logging.*;

import common.AtfLogger;
import common.CommandObject;
import common.LocalDateTimePair;
import common.TaskObject;

import static logic.constants.Index.*;
import static logic.constants.Strings.*;

/**
 * An abstract class. Inherited by Done, Incomplete and Overdue. This set of
 * classes serve to facilitate switching the status of a selected task between
 * three set statuses - "done", "incomplete" and "overdue". <br>
 * Precondition: This command should be preceded by a command such as "display"
 * or "search", which will generate a list of tasks for the user to manipulate.
 * Class methods will not work if the last output task list is invalid. <br>
 * Contains the abstract run() method which will be overridden by each
 * subclasses's implementation.
 * 
 * @author ChongYan
 *
 */
public abstract class Mark {

	protected static Logger logger = AtfLogger.getLogger();

	/**
	 * @param TaskObject
	 *            instructionTask - This is the TaskObject which contains
	 *            information on which task's status to be modified.
	 * @param TaskObject
	 *            markedTask - This is a copy of the TaskObject which is
	 *            modified by this specific Mark object. Will be added to the
	 *            undo/redo list whichever is applicable.
	 * @param String
	 *            taskName - This contains the name of the task which was
	 *            modified
	 * @param String
	 *            statusBeforeChange - This contains the status of the task
	 *            before it was toggled by Mark.run()
	 * @param taskIdToMark
	 *            - Contains the task ID of the task whose status will be
	 *            toggled
	 */

	protected TaskObject originalTask = new TaskObject(); // original task info for undo purposes
	protected ArrayList<LocalDateTimePair> originalTimings = new ArrayList<LocalDateTimePair>();

	protected TaskObject markedTask;
	protected String taskName = "";
	protected String statusBeforeChange = "";
	protected ArrayList<TaskObject> taskList;
	protected ArrayList<TaskObject> lastOutputTaskList;
	protected ArrayList<String> output = new ArrayList<String>();
	protected int taskIdToMark = -1; // The intended task ID user wants to mark
	protected int mostRecentlyMarkedTaskId = -1; 
	// The task ID of the task most recently marked complete, which is split task for recurring
	protected int command;

	protected TaskObject markTaskObj = new TaskObject();
	protected int index = -1;
	protected boolean isExceptionThrown = false;

	public Mark() {

	}

	/**
	 * Generic constructor for all subclasses of Mark to be used.
	 * 
	 * @param taskObj
	 *            - Contains information on the task to be changed, not the task to be changed
	 * @param taskList
	 *            - Contains all existing tasks in Adult TaskFinder
	 * @param lastOutputTaskList
	 *            - Contains the list of tasks which was last output
	 */
	public Mark(CommandObject commandObj, ArrayList<TaskObject> taskList,
			ArrayList<TaskObject> lastOutputTaskList) {
		this.index = commandObj.getIndex();
		this.taskList = taskList;
		this.lastOutputTaskList = lastOutputTaskList;
		this.command = commandObj.getCommandType();
	}

	public abstract ArrayList<String> run();

	// May need to change if parser changes the way this command object is constructed
	protected void obtainTaskId() {
		index--;
		if (index >= 0 && index < lastOutputTaskList.size()) {
			taskIdToMark = lastOutputTaskList.get(index).getTaskId();
			logger.log(Level.INFO, "valid task ID obtained");
		} else {
			processError();
			logger.log(Level.WARNING, "invalid task ID obtained");
		}
	}
	
	private void processError() {
		if (command == INDEX_COMPLETE) {
			createErrorOutput(MESSAGE_MARK_DONE_ERROR);
		} else if (command == INDEX_INCOMPLETE) {
			createErrorOutput(MESSAGE_MARK_INCOMPLETE_ERROR);
		}
	}

	protected void checkCurrentStatus(String status) throws MarkException {
		if (markedTask.getStatus().equals(status)) {
			MarkException e = new MarkException(markedTask);
			throw e;
		}
	}

	protected void saveToFile() {
		IStorage storage = FileStorage.getInstance();
		try {
			storage.save(taskList);
			logger.log(Level.INFO, "successfully saved changes to external file");
		} catch (NoSuchFileException e) {
			e.printStackTrace();
			createErrorOutput(String.format(MESSAGE_SAVE_ERROR, "default location"));
			logger.log(Level.WARNING, "unable to save changes to external file");
		} catch (IOException e) {
			e.printStackTrace();
			createErrorOutput(String.format(MESSAGE_SAVE_ERROR, "default location"));
			logger.log(Level.WARNING, "unable to save changes to external file");
		}
	}

	protected abstract boolean changeStatus();

	protected void createErrorOutput(String errorMessage) {
		if (output.isEmpty()) {
			output.add(errorMessage);
		}
	}

	protected void deleteSplitTaskFromTaskList() {
		int smallestTaskId = 0;
		int indexOfTaskToDelete = -1;

		for (int i = 0; i < taskList.size(); i++) {
			int taskId = taskList.get(i).getTaskId();
			if (taskId < 0 && taskId < smallestTaskId) {
				smallestTaskId = taskId;
				indexOfTaskToDelete = i;
			}
		}

		taskList.remove(indexOfTaskToDelete);
		logger.log(Level.INFO, "deleted split task from task list");
	}

	// ---------------------------- GETTERS AND SETTERS ---------------------------- 
	
	public int getTaskIdToMark() {
		return taskIdToMark;
	}

	public String getStatusToChange() {
		return statusBeforeChange;
	}

	public TaskObject getMarkedTask() {
		return markedTask;
	}

	public TaskObject getOriginalTask() {
		return originalTask;
	}
	
	public int getMostRecentlyMarkedTaskId() {
		return mostRecentlyMarkedTaskId;
	}

	public int getMarkIndex() {
		return index;
	}

	public boolean getIsExceptionThrown() {
		return isExceptionThrown;
	}
	
	public void setMarkIndex(int index) {
		this.index = index;
	}
	
	public void setMostRecentlyMarkedTaskId(int mostRecentlyMarkedTaskId) {
		this.mostRecentlyMarkedTaskId = mostRecentlyMarkedTaskId;
	}
}
