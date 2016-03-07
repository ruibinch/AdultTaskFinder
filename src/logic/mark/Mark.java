package logic.mark;

import logic.*;

import java.util.ArrayList;

import common.TaskObject;

/**
 * An abstract class. Inherited by Done, Incomplete and Overdue. This set of classes 
 * serve to facilitate switching the status of a selected task between three set statuses - 
 * "done", "incomplete" and "overdue".
 * <br> Precondition: This command should be preceded by a command such as "display" or 
 * "search", which will generate a list of tasks for the user to manipulate. 
 * Class methods will not work if the last output task list is invalid.
 * <br> Contains the abstract run() method which will be overridden by each subclasses's 
 * implementation.
 * @author ChongYan
 *
 */
public abstract class Mark {

	private final String MESSAGE_ERROR = "Error marking task as complete";

	/**
	 * @param TaskObject instructionTask - This is the TaskObject which contains information 
	 * on which task's status to be modified.
	 * @param TaskObject markedTask - This is a copy of the TaskObject which is modified by
	 * this specific Mark object. Will be added to the undo/redo list whichever is 
	 * applicable.
	 * @param String taskName - This contains the name of the task which was modified
	 * @param String statusBeforeChange - This contains the status of the task before it
	 * was toggled by Mark.run()
	 * @param taskIdToMark - Contains the task ID of the task whose status will be toggled
	 */
	protected TaskObject instructionTask; // Task containing instruction
	protected TaskObject markedTask;
	protected String taskName = "";
	protected String statusBeforeChange = "";
	protected ArrayList<TaskObject> taskList;
	protected ArrayList<TaskObject> lastOutputTaskList;
	protected ArrayList<String> output = new ArrayList<String>();
	protected int taskIdToMark = -1; // The intended task ID user wants to mark

	public Mark() {

	}
	
	/**
	 * Generic constructor for all subclasses of Mark to be used.
	 * @param taskObj - Contains information on the task to be changed, not the task to be changed
	 * @param taskList - Contains all existing tasks in Adult TaskFinder
	 * @param lastOutputTaskList - Contains the list of tasks which was last outputted
	 */
	public Mark(TaskObject taskObj, ArrayList<TaskObject> taskList, ArrayList<TaskObject> lastOutputTaskList) {
		instructionTask = taskObj;
		this.taskList = taskList;
		this.lastOutputTaskList = lastOutputTaskList;
	}

	public abstract ArrayList<String> run();

	// May need to change if parser changes the way this command object is constructed
	protected void obtainTaskId() {
		int lineNumber = Integer.parseInt(instructionTask.getTitle());
		lineNumber--;
		if (lineNumber >= 0 && lineNumber < lastOutputTaskList.size()) {
			taskIdToMark = lastOutputTaskList.get(lineNumber).getTaskId();
		} else {
			createErrorOutput();
		}
	}

	protected boolean changeStatus() {
		for (int i = 0; i < taskList.size(); i++) {
			if (taskList.get(i).getTaskId() == taskIdToMark) {
				taskName = taskList.get(i).getTitle();
				statusBeforeChange = taskList.get(i).getStatus();
				markedTask = taskList.get(i);
				taskList.get(i).setStatus("completed");
				return true;
			}
		}
		return false;
	}

	protected void createErrorOutput() {
		output.add(MESSAGE_ERROR);
	}

	// Getter
	public int getTaskIdToMark() {
		return taskIdToMark;
	}

	public String getStatusToChange() {
		return statusBeforeChange;
	}

	public TaskObject getMarkedTask() {
		return markedTask;
	}
}
