package logic.add;

import logic.*;
import logic.mark.*;
import storage.*;
import common.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.time.DateTimeException;
import java.time.LocalDateTime;

import common.Interval;
import common.TaskObject;

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

	private final String MESSAGE_ADD = "Task added: ";
	private final String MESSAGE_FAIL = "Failed to add task. ";
	private final String MESSAGE_CLASH = "Task: %1s clashes with %2s";
	private final String MESSAGE_INVALID_TIME = "Reason: Invalid time input.";
	private final String MESSAGE_NULL_POINTER = "Reason: No object available to access.";

	private TaskObject task;
	private int index;
	private boolean addedInternal = false;
	private boolean addedExternal = false;
	private boolean isClash = false;
	private ArrayList<TaskObject> taskList;
	private ArrayList<String> output = new ArrayList<String>();
	private ArrayList<TaskObject> clashedTasks = new ArrayList<TaskObject>();

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
		try {
			String taskType = task.getCategory();
			if (taskType.equals("event")) {
				assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
				assert (!task.getEndDateTime().equals(LocalDateTime.MAX));

				LocalDateTimePair taskTime = new LocalDateTimePair(task.getStartDateTime(), task.getEndDateTime());
				task.addToTaskDateTime(taskTime);
				// check for clash only necessary if task is an event
				if (task.getIsRecurring()) {
					addRecurringTimes();
				}
				isClash = checkIfClash();
				addTask();
			} else {
				if (taskType.equals("deadline")) {
					assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
					assert (task.getEndDateTime().equals(LocalDateTime.MAX));

					LocalDateTimePair taskTime = new LocalDateTimePair(task.getStartDateTime());
					task.addToTaskDateTime(taskTime);
					boolean isOverdue = checkIfOverdue();
					if (task.getIsRecurring()) {
						addRecurringTimes();
					}
					if (isOverdue) {
						setTaskStatus(isOverdue);
					}
					addTask();
				} else {
					assert (taskType.equals("floating"));
					assert (task.getStartDateTime().equals(LocalDateTime.MAX));
					assert (task.getEndDateTime().equals(LocalDateTime.MAX));

					addTask();
					// Recurrence not possible for floating tasks
				}
			}
			createOutput();
		} catch (DateTimeException e) {
			output.add(MESSAGE_FAIL + MESSAGE_INVALID_TIME);
		} catch (NullPointerException e) {
			e.printStackTrace();
			output.add(MESSAGE_FAIL + MESSAGE_NULL_POINTER);
		}
		return output;
	}

	private void addRecurringTimes() {
		// Generates 10 additional times on top of the first
		LocalDateTime startTimeObject = task.getStartDateTime();
		LocalDateTime endTimeObject = task.getEndDateTime();
		for (int i = 0; i < 10; i++) {
			startTimeObject = addInterval(startTimeObject, task.getInterval());
			if (task.getCategory().equals("event")) {
				endTimeObject = addInterval(endTimeObject, task.getInterval());
			} else {
				endTimeObject = LocalDateTime.MAX;
			}
			LocalDateTimePair nextTime = new LocalDateTimePair(startTimeObject, endTimeObject);
			task.addToTaskDateTime(nextTime);
			// System.out.println(i+1+ " "+ startTimeObject.toString());
		}
	}

	public static LocalDateTime addInterval(LocalDateTime originalTime, Interval interval) {
		LocalDateTime newTime = originalTime;
		newTime = newTime.plusYears(interval.getYear());
		newTime = newTime.plusMonths(interval.getMonth());
		newTime = newTime.plusWeeks(interval.getWeek());
		newTime = newTime.plusDays(interval.getDay());
		newTime = newTime.plusHours(interval.getHour());
		newTime = newTime.plusMinutes(interval.getMinute());
		newTime = newTime.plusSeconds(interval.getSecond());
		return newTime;
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
		LocalDateTime deadline = task.getStartDateTime(); // Depends on parser's
															// allocation
		if (deadline.isBefore(LocalDateTime.now())) {
			isOverdue = true;
		}
		return isOverdue;
	}

	private void setTaskStatus(boolean isOverdue) {
		if (isOverdue) {
			task.setStatus("overdue");
		}
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
		return hasClashes;
	}

	private boolean checkAcrossAllTimes(TaskObject current, int i) throws NullPointerException {
		boolean hasClashes = false;
		for (int j = 0; j < task.getTaskDateTime().size(); j++) {
			for (int k = 0; k < current.getTaskDateTime().size(); k++) {
				LocalDateTime currentStart = current.getTaskDateTime().get(k).getStartDateTime();
				LocalDateTime currentEnd = current.getTaskDateTime().get(k).getEndDateTime();
				LocalDateTime newStart = task.getTaskDateTime().get(j).getStartDateTime();
				LocalDateTime newEnd = task.getTaskDateTime().get(j).getEndDateTime();
				if (checkTimeClash(currentStart, currentEnd, newStart, newEnd)) {
					if (task.getIsRecurring() || current.getIsRecurring()) {
						addClashedRecurringTasks(current);
					} else {
						clashedTasks.add(taskList.get(i));
					}
					hasClashes = true;
				}
			}
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
	private boolean checkTimeClash(LocalDateTime currentStart, LocalDateTime currentEnd, LocalDateTime newStart,
			LocalDateTime newEnd) throws DateTimeException {

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

		return false;
	}

	private void addClashedRecurringTasks(TaskObject current) {
		for (int i = 0; i < clashedTasks.size(); i++) {
			if (clashedTasks.get(i).equals(current)) {
				// To prevent duplicate tasks from being added to clashedTasks
				return;
			}
		}
		clashedTasks.add(current);
	}

	private void addTask() {
		addInternal();
		addExternal();
	}

	private void addInternal() throws NullPointerException {
		int originalSize = taskList.size();
		int newSize = originalSize + 1;
		if (index != -1) { // must add at a specific point
			taskList.add(index - 1, task);
		} else {
			taskList.add(task);
		}

		if (taskList.size() == newSize)
			addedInternal = true;
	}

	private void addExternal() {
		Storage storage = FileStorage.getInstance();
		try {
			storage.save(taskList);
		} catch (NoSuchFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
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
		} else {
			output.add(MESSAGE_FAIL);
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
