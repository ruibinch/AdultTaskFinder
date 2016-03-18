package logic.add;

import logic.*;
import storage.*;
import common.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.logging.*;

import common.TaskObject;

import static logic.constants.Index.*;
import static logic.constants.Strings.*;

/**
 * Creates an "Add" object to facilitate adding of a task into Adult
 * TaskFinder's TaskList. Tasks will be added internally before being saved to
 * its default file location. <br>
 * Events to be added will be checked against all existing events - the names of
 * clashing events will be generated in the output list, but the user will not
 * be stopped from adding the event. <br>
 * Deadlines to be added will be checked if it is already overdue - overdue
 * deadlines will have their status toggled to "overdue" before being added.
 * 
 * @author ChongYan
 *
 */
public class Add {

	private TaskObject task;
	private int index;
	private boolean addedInternal = false;
	private boolean addedExternal = false;
	private boolean isClash = false;
	private ArrayList<TaskObject> taskList;
	private ArrayList<String> output = new ArrayList<String>();
	private ArrayList<TaskObject> clashedTasks = new ArrayList<TaskObject>();

	public static Logger logger = Logger.getLogger("Add");

	public Add() {

	}

	/**
	 * Normal constructor for Add
	 * 
	 * @param taskObj
	 *            The task to be added, generated by parser.
	 * @param index
	 *            The position within the task list to add the task. Optional,
	 *            default value 0.
	 * @param taskList
	 *            The list of tasks maintained internally by Adult TaskFinder
	 */
	public Add(TaskObject taskObj, int index, ArrayList<TaskObject> taskList) {
		this.task = taskObj;
		this.index = index;
		this.taskList = taskList;
	}

	/**
	 * Called by logic to add the task initialised in the Add object to the task
	 * list.
	 * 
	 * @return output: ArrayList<String> - Contains all the output that the user
	 *         will see
	 */
	public ArrayList<String> run() {
		assert (!task.equals(null));
		logger.log(Level.INFO, "going to start processing task for adding");
		try {
			String taskType = task.getCategory();
			if (taskType.equals("event")) {
				assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
				assert (!task.getEndDateTime().equals(LocalDateTime.MAX));
				logger.log(Level.INFO, "event to be added");

				// check for clash only necessary if task is an event
				isClash = checkIfClash();
				addTask();
			} else {
				if (taskType.equals("deadline")) {
					assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
					assert (task.getEndDateTime().equals(LocalDateTime.MAX));

					logger.log(Level.INFO, "deadline to be added");

					boolean isOverdue = checkIfOverdue();
					if (isOverdue) {
						setTaskStatus(isOverdue);
					}
					addTask();
				} else {
					assert (taskType.equals("floating"));
					assert (task.getStartDateTime().equals(LocalDateTime.MAX));
					assert (task.getEndDateTime().equals(LocalDateTime.MAX));

					logger.log(Level.INFO, "floating to be added");

					addTask();
					// Recurrence not possible for floating tasks
				}
			}
			createOutput();
		} catch (DateTimeException e) {
			output.add(MESSAGE_FAIL + MESSAGE_INVALID_TIME);
			logger.log(Level.WARNING, "date within input task is invalid");
		} catch (NullPointerException e) {
			e.printStackTrace();
			output.add(MESSAGE_FAIL + MESSAGE_NULL_POINTER);
			logger.log(Level.WARNING, "tried to retrieve an unavailable object");
		}
		return output;
	}

	/**
	 * Throws Exception if time input is invalid. Deadline to be added has a
	 * valid date and time in its TaskObject <br>
	 * Converts date and time to the LocalDateTime format by calling static
	 * method in Logic class.
	 * 
	 * @return isOverdue: boolean - True if deadline is before current time
	 */
	private boolean checkIfOverdue() throws DateTimeException {
		boolean isOverdue = false;
		// LocalDateTime deadline = task.getStartDateTime();

		logger.log(Level.INFO, "going to check whether a deadline is overdue");

		if (task.getStartDateTime().isBefore(LocalDateTime.now())) {
			isOverdue = true;
		}
		return isOverdue;
	}

	private void setTaskStatus(boolean isOverdue) {
		if (isOverdue) {
			task.setStatus("overdue");
		}
		logger.log(Level.INFO, "toggled a task's status if applicable");
	}

	private boolean checkIfClash() throws NullPointerException {
		boolean hasClashes = false;
		for (int i = 0; i < taskList.size(); i++) {
			if (taskList.get(i).getCategory().equals("event")) {
				if (checkAcrossAllTimes(taskList.get(i), i)) {
					hasClashes = true;
				}
			}
		}
		logger.log(Level.INFO, "checked if events clash");
		return hasClashes;
	}

	private boolean checkAcrossAllTimes(TaskObject current, int i) throws NullPointerException {
		boolean hasClashes = false;
				if (checkTimeClash(current)) {
						clashedTasks.add(taskList.get(i));
						logger.log(Level.INFO, "detected a clash between non-recurring tasks");
					
					hasClashes = true;
				}
		return hasClashes;
	}

	/**
	 * LocalDateTime format obtained by calling static method in Logic class
	 * Checks if two events clash. Achieves this by: <br>
	 * 1) Checking if event 1's start time is between event 2's start and end
	 * time <br>
	 * 2) Checking if event 1's end time is between event 2's start and end time
	 * <br>
	 * 3) Checking if event 2's start time is between event 1's start and end
	 * time <br>
	 * 4) Checking if event 2's end time is between event 1's start and end time
	 * 
	 * @param current
	 *            The TaskObject passed into the function from the task list.
	 * @return
	 */
	private boolean checkTimeClash(TaskObject current) throws DateTimeException {
		
		LocalDateTime currentStart = current.getStartDateTime();
		LocalDateTime currentEnd = current.getEndDateTime();
		LocalDateTime newStart = task.getStartDateTime();
		LocalDateTime newEnd = task.getEndDateTime();

		if (currentStart.isAfter(newStart) || currentStart.isEqual(newStart)) {
			if (currentStart.isBefore(newEnd) || currentStart.isEqual(newEnd)) {
				return true;
			}
		}
		if (currentEnd.isAfter(newStart) || currentEnd.isEqual(newStart)) {
			if (currentEnd.isBefore(newEnd) || currentEnd.isEqual(newEnd)) {
				return true;
			}
		}
		if (newStart.isAfter(currentStart) || newStart.isEqual(currentStart)) {
			if (newStart.isBefore(currentEnd) || newStart.isEqual(currentEnd)) {
				return true;
			}
		}
		if (newEnd.isAfter(currentStart) || newEnd.isEqual(currentStart)) {
			if (newEnd.isBefore(currentEnd) || newEnd.isEqual(currentEnd)) {
				return true;
			}
		}

		logger.log(Level.INFO, "no clash detected between two timings");

		return false;
	}

	private void addTask() {
		addInternal();
		addExternal();
		logger.log(Level.INFO, "added tasks to the taskList");
	}

	private void addInternal() throws NullPointerException {
		int originalSize = taskList.size();
		int newSize = originalSize + 1;
		if (index != -1) { // must add at a specific point
			taskList.add(index - 1, task);
		} else {
			taskList.add(task);
		}

		if (taskList.size() == newSize) {
			addedInternal = true;
			logger.log(Level.INFO, "added task to internal taskList");
		}
		logger.log(Level.WARNING, "failed to add task");
	}

	private void addExternal() {
		IStorage storage = FileStorage.getInstance();
		try {
			storage.save(taskList);
			logger.log(Level.INFO, "added task to external file storage");
		} catch (NoSuchFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "did not manage to add task externally, invalid file");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "did not manage to add task externally, IO exception");
		}
		addedExternal = true;

	}

	private void createOutput() {
		if (addedInternal && addedExternal) {
			String title = task.getTitle();
			String text = MESSAGE_ADD.concat(title);
			output.add(text);
			if (isClash) {
				for (int i = 0; i < clashedTasks.size(); i++) {
					String clashMessage = createClashOutput(i);
					output.add(clashMessage);
				}
			}
			logger.log(Level.INFO, "output created successfully");
		} else {
			output.add(MESSAGE_FAIL);
			logger.log(Level.WARNING, "task was not added, failure output created");
		}
	}

	private String createClashOutput(int i) {
		String text = "";
		text = String.format(MESSAGE_CLASH, task.getTitle(), clashedTasks.get(i).getTitle());
		// NEED A BETTER WAY TO OUTPUT CLASHES
		return text;
	}

	// GETTERS, SETTERS
	public ArrayList<String> getOutput() {
		return output;
	}

	public ArrayList<TaskObject> getTaskList() {
		return taskList;
	}

	public TaskObject getTask() {
		return task;
	}

	public void setOutput(ArrayList<String> output) {
		this.output = output;
	}

	public void setTaskList(ArrayList<TaskObject> taskList) {
		this.taskList = taskList;
	}

	public void setTask(TaskObject task) {
		this.task = task;
	}
}
