package logic.add;

import logic.*;
import logic.exceptions.RecurrenceException;
import storage.*;
import common.*;

import java.io.IOException;
import java.nio.file.NoSuchFileException;
import java.util.ArrayList;
import java.time.DateTimeException;
import java.time.LocalDateTime;
import java.util.logging.*;
import java.io.File;

import common.TaskObject;

import static logic.constants.Index.*;
import static logic.constants.Strings.*;

/**
 * Creates an "Add" object to facilitate adding of a task into Adult TaskFinder's TaskList. Tasks will be
 * added internally before being saved to its default file location. <br>
 * Events to be added will be checked against all existing events - the names of clashing events will be
 * generated in the output list, but the user will not be stopped from adding the event. <br>
 * Deadlines to be added will be checked if it is already overdue - overdue deadlines will have their status
 * toggled to "overdue" before being added.
 * 
 * @author ChongYan, RuiBin
 *
 */
public class Add {

	private TaskObject task;
	private int index;
	private int lastSearchedIndex;
	private boolean addedInternal = false;
	private boolean addedExternal = false;
	private boolean isClash = false;
	private boolean isOverdue = false;
	private ArrayList<TaskObject> taskList;
	private ArrayList<String> output = new ArrayList<String>();
	private ArrayList<TaskObject> clashedTasks = new ArrayList<TaskObject>();

	private boolean isAddSingleOccurrence = false;
	private boolean isEvent = false;
	private boolean isDeadline = false;
	private boolean isFloating = false;

	private static Logger logger = AtfLogger.getLogger();

	public Add() {

	}

	/**
	 * Normal constructor for Add
	 * 
	 * @param taskObj
	 *            The task to be added, generated by parser.
	 * @param index
	 *            The position within the task list to add the task. Optional, default value 0.
	 * @param taskList
	 *            The list of tasks maintained internally by Adult TaskFinder
	 */
	public Add(TaskObject taskObj, int index, int lastSearchedIndex, ArrayList<TaskObject> taskList) {
		this.task = taskObj;
		this.index = index;
		this.lastSearchedIndex = lastSearchedIndex;
		this.taskList = taskList;
	}

	public Add(TaskObject taskObj, int index, ArrayList<TaskObject> taskList) {
		this.task = taskObj;
		this.index = index;
		this.taskList = taskList;
	}

	/**
	 * Called by logic to add the task initialised in the Add object to the task list.
	 * 
	 * @return output: ArrayList<String> - Contains all the output that the user will see
	 */
	public ArrayList<String> run() {
		// Special processing to handle undoing the deletion of an occurrence of
		// a recurring task
		if (task.getIsContainingOnlyTaskDateTimes()) {
			addSingleOccurrence();
		} else {
			assert (!task.getTitle().equals(""));
			// setUpLogger();
			try {
				determineTaskCategory();
				processTaskInformation();
				addTask();
				saveToStorage();
				createOutput();
			} catch (DateTimeException e) {
				output.add(MESSAGE_FAIL + MESSAGE_INVALID_TIME);
				logger.log(Level.WARNING, "date within input task is invalid");
			} catch (NullPointerException e) {
				e.printStackTrace();
				output.add(MESSAGE_FAIL + MESSAGE_NULL_POINTER);
				logger.log(Level.WARNING, "tried to retrieve an unavailable object");
			} catch (Exception e) {
				e.printStackTrace();
				output.add(MESSAGE_FAIL);
				logger.log(Level.WARNING, "task does not have a valid category");
			}
		}
		return output;
	}

	private void determineTaskCategory() {
		if (task.getCategory().equals(CATEGORY_EVENT)) {
			this.isEvent = true;
		}
		if (task.getCategory().equals(CATEGORY_DEADLINE)) {
			this.isDeadline = true;
		}
		if (task.getCategory().equals(CATEGORY_FLOATING)) {
			this.isFloating = true;
		}
	}

	/**
	 * Control flow to determine adding process for each type of task
	 * 
	 * @throws Exception
	 */
	private void processTaskInformation() throws Exception {
		if (isEvent) {
			assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
			assert (!task.getEndDateTime().equals(LocalDateTime.MAX));
			logger.log(Level.INFO, "event to be added");
			processEventDetails();
		}
		if (isDeadline) {
			assert (!task.getStartDateTime().equals(LocalDateTime.MAX));
			assert (task.getEndDateTime().equals(LocalDateTime.MAX));
			logger.log(Level.INFO, "deadline to be added");
			processDeadlineDetails();
		}
		if (isFloating) {
			assert (task.getStartDateTime().equals(LocalDateTime.MAX));
			assert (task.getEndDateTime().equals(LocalDateTime.MAX));
			logger.log(Level.INFO, "floating to be added");
		}
		if (!isEvent && !isDeadline && !isFloating) {
			Exception e = new Exception("Invalid task");
			throw e;
		}
	}

	/*****************************************************************************/
	/**
	 * Checks for clashes between events (including recurrent times) and adds to taskList Also creates all
	 * dates and times for recurrent tasks
	 */
	private void processEventDetails() {
		try {
			this.isOverdue = checkIfOverdue();
			copyToTaskDateTimeList(task.getStartDateTime(), task.getEndDateTime());
			if (task.getIsRecurring()) {
				addRecurringEventTimes();
				removeAnyDeletedOccurrences();
			}
			if (isOverdue) {
				if (task.getIsRecurring()) {
					Recurring.updateEvent(task, taskList, STATUS_OVERDUE);
				} else {
					setTaskStatus(isOverdue);
				}
			}
			checkIfEventsClash();
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			output.add(exceptionMessage);
		}
	}

	/**
	 * Copies startDateTime and endDateTime to taskDateTimes
	 * 
	 * @param startDateTime
	 * @param endDateTime
	 */
	private void copyToTaskDateTimeList(LocalDateTime startDateTime, LocalDateTime endDateTime) {
		LocalDateTimePair pair = new LocalDateTimePair(startDateTime, endDateTime);
		task.addToTaskDateTimes(pair);
	}

	private void addRecurringEventTimes() {
		try {
			Recurring.setAllRecurringEventTimes(task);
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			output.add(exceptionMessage);
		}
	}

	private void removeAnyDeletedOccurrences() {
		ArrayList<LocalDateTimePair> deletedOccurrences = task.getDeletedTaskDateTimes();
		LocalDateTimePair taskCurrentStartEndDateTime = new LocalDateTimePair(task.getStartDateTime(),
				task.getEndDateTime());

		try {
			for (int i = 0; i < deletedOccurrences.size(); i++) {
				logger.log(Level.INFO, "Removing occurrence that had been previously deleted");
				LocalDateTimePair deletedOccurrence = deletedOccurrences.get(i);

				for (int j = 0; j < task.getTaskDateTimes().size(); j++) {
					if (task.getTaskDateTimes().get(j).equals(deletedOccurrence)
							&& !task.getTaskDateTimes().get(j).equals(taskCurrentStartEndDateTime)) {
						task.getTaskDateTimes().remove(j);
					}
				}
			}
		} catch (IndexOutOfBoundsException e) {

		}
	}

	/***********************************************************************************/
	/**
	 * Checks if a deadline is overdue, modifies status if necessary, adds to taskList
	 */
	private void processDeadlineDetails() {
		try {
			this.isOverdue = checkIfOverdue();
			copyToTaskDateTimeList(task.getStartDateTime(), task.getEndDateTime());
			if (task.getIsRecurring()) {
				addRecurringDeadlineTimes(task);
			}
			if (isOverdue) {
				if (task.getIsRecurring()) {
					Recurring.updateDeadline(task, taskList, STATUS_OVERDUE);
				} else {
					setTaskStatus(isOverdue);
				}
			}
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			output.add(exceptionMessage);
		}
	}

	private void addRecurringDeadlineTimes(TaskObject task) {
		try {
			Recurring.setAllRecurringDeadlineTimes(task);
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			output.add(exceptionMessage);
		}
	}

	/**
	 * Throws Exception if time input is invalid. Deadline to be added has a valid date and time in its
	 * TaskObject <br>
	 * Converts date and time to the LocalDateTime format by calling static method in Logic class.
	 * 
	 * @return isOverdue: boolean - True if deadline is before current time
	 */
	private boolean checkIfOverdue() throws DateTimeException {
		boolean isOverdue = false;

		logger.log(Level.INFO, "going to check whether a deadline is overdue");

		if (task.getStartDateTime().isBefore(LocalDateTime.now())) {
			isOverdue = true;
		}
		return isOverdue;
	}

	private void setTaskStatus(boolean isOverdue) {
		if (isOverdue) {
			task.setStatus(STATUS_OVERDUE);
		}
		logger.log(Level.INFO, "toggled a task's status if applicable");
	}

	/*********************************************************************************/
	/**
	 * Group of functions checking for clashes between events.
	 */

	// Checks with incomplete, overdue events for clashes
	private void checkIfEventsClash() throws NullPointerException {
		if (task.getStartDateTime().isAfter(task.getEndDateTime())) {
			DateTimeException e = new DateTimeException("Start Date Time after End Date Time");
			throw e;
		}
		for (int i = 0; i < taskList.size(); i++) {
			if (taskList.get(i).getCategory().equals(CATEGORY_EVENT)) {
				if (!taskList.get(i).getStatus().equals(STATUS_COMPLETED)) {
					checkAllExistingTimes(taskList.get(i));
				}
			}
		}
		logger.log(Level.INFO, "checked if events clash");
	}

	private void checkAllExistingTimes(TaskObject current) throws NullPointerException {
		ArrayList<LocalDateTimePair> currentTaskDateTimes = current.getTaskDateTimes();
		ArrayList<LocalDateTimePair> newTaskDateTimes = task.getTaskDateTimes();

		for (int i = 0; i < currentTaskDateTimes.size(); i++) {
			for (int j = 0; j < newTaskDateTimes.size(); j++) {
				processIndividualClashes(i, j, currentTaskDateTimes, newTaskDateTimes, current);
			}
		}
	}

	private void processIndividualClashes(int currentIndex, int newIndex,
			ArrayList<LocalDateTimePair> currentTaskDateTimes, ArrayList<LocalDateTimePair> newTaskDateTimes,
			TaskObject current) {

		LocalDateTime currentStart = currentTaskDateTimes.get(currentIndex).getStartDateTime();
		LocalDateTime currentEnd = currentTaskDateTimes.get(currentIndex).getEndDateTime();
		LocalDateTime newStart = newTaskDateTimes.get(newIndex).getStartDateTime();
		LocalDateTime newEnd = newTaskDateTimes.get(newIndex).getEndDateTime();

		if (checkIndividualTimeClash(currentStart, currentEnd, newStart, newEnd)) {
			this.isClash = true;
			addToClashedTasks(current);
			logger.log(Level.INFO, "detected a clash between non-recurring tasks");
		}
	}

	/**
	 * LocalDateTime format obtained by calling static method in Logic class Checks if two events clash.
	 * Achieves this by: <br>
	 * 1) Checking if event 1's start time is between event 2's start and end time <br>
	 * 2) Checking if event 1's end time is between event 2's start and end time <br>
	 * 3) Checking if event 2's start time is between event 1's start and end time <br>
	 * 4) Checking if event 2's end time is between event 1's start and end time
	 * 
	 * @param current
	 *            The TaskObject passed into the function from the task list.
	 * @return
	 */
	private boolean checkIndividualTimeClash(LocalDateTime currentStart, LocalDateTime currentEnd,
			LocalDateTime newStart, LocalDateTime newEnd) throws DateTimeException {

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

	/********************************************************************************/
	/**
	 * Group of functions for addition of task
	 */

	// For processing undo of deletion of a single occurrence
	private void addSingleOccurrence() {
		ArrayList<LocalDateTimePair> timings = task.getTaskDateTimes();
		assert (timings.size() == 1); // it should only store 1 occurrence of
										// timings
		assert (lastSearchedIndex != -1);

		LocalDateTimePair occurrenceToBeAdded = timings.get(0);
		TaskObject taskToBeModified = taskList.get(lastSearchedIndex - 1);
		taskToBeModified.getTaskDateTimes().add(index - 1, occurrenceToBeAdded);

		// updates the startDateTime and endDateTime to that of the occurrence
		// that has been added back
		taskToBeModified.updateStartAndEndDateTimes();
		isAddSingleOccurrence = true;

	}

	private void addTask() throws NullPointerException {
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
		} else {
			logger.log(Level.WARNING, "failed to add task");
		}
	}

	private void saveToStorage() {
		IStorage storage = FileStorage.getInstance();
		try {
			storage.save(taskList);
			logger.log(Level.INFO, "added task to external file storage");
		} catch (NoSuchFileException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "did not manage to add task externally, invalid file");
			output.add(MESSAGE_REQUEST_SAVE_LOCATION);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			logger.log(Level.WARNING, "did not manage to add task externally, IO exception");
		}
		addedExternal = true;

	}

	/****************************************************************************/
	/**
	 * Group of functions for creating output.
	 */

	private void createOutput() {
		if (addedInternal && addedExternal) {
			String title = task.getTitle();
			String text = MESSAGE_ADD.concat(title).concat(". ");
			if (task.getIsRecurring()) {
				text = "Recurring ".concat(text);
				// "Recurring task added: -task-. "
			}
			if (isClash) {
				text = concatenateClashOutput(text);
			}
			if (isOverdue) {
				text = text.concat(MESSAGE_ADD_OVERDUE);
			}
			output.add(text);
			logger.log(Level.INFO, "output created successfully");
		} else {
			if (output.isEmpty()) {
				output.add(MESSAGE_FAIL);
				logger.log(Level.WARNING, "task was not added, failure output created");
			}
		}
	}

	private String concatenateClashOutput(String text) {
		String clashFormat = String.format(MESSAGE_CLASH, task.getTitle());
		text = text.concat(clashFormat);
		for (int i = 0; i < clashedTasks.size(); i++) {
			String title = clashedTasks.get(i).getTitle() + ", ";
			text = text.concat(title);
		}
		text = text.substring(0, text.length() - 2);
		text = text.concat(". ");
		return text;
	}

	/***************************************************************************/

	private void addToClashedTasks(TaskObject current) {
		boolean canAdd = true;
		for (int i = 0; i < clashedTasks.size(); i++) {
			if (clashedTasks.get(i).getTaskId() == current.getTaskId()) {
				canAdd = false;
			}
		}
		if (canAdd) {
			clashedTasks.add(current);
		}
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

	public boolean getIsClash() {
		return isClash;
	}

	public boolean getIsAddSingleOccurrence() {
		return isAddSingleOccurrence;
	}

	public ArrayList<TaskObject> getClashedTasks() {
		return clashedTasks;
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
