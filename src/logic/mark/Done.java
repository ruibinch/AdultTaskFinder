package logic.mark;

import java.util.ArrayList;
import java.util.logging.*;

import common.CommandObject;
import common.LocalDateTimePair;
import common.TaskObject;
import logic.Recurring;
import logic.exceptions.RecurrenceException;

import static logic.constants.Index.*;
import static logic.constants.Strings.*;

/**
 * Creates a Done object, which is a Mark object. However, it specifically
 * changes the status of the target task to "done".
 * 
 * @author ChongYan
 *
 */
public class Done extends Mark {

	/**
	 * Constuctor for a Done object.
	 * 
	 * @param commandObj
	 *            - Contains information on the task to be changed, not the task
	 *            to be changed
	 * @param taskList
	 *            - Contains all existing tasks in Adult TaskFinder
	 * @param lastOutputTaskList
	 *            - Contains the list of tasks which was last outputted
	 */
	public Done(CommandObject commandObj, ArrayList<TaskObject> taskList, ArrayList<TaskObject> lastOutputTaskList) {
		this.index = commandObj.getIndex();
		this.markTaskObj = commandObj.getTaskObject();
		this.taskList = taskList;
		this.lastOutputTaskList = lastOutputTaskList;
	}

	/**
	 * Main method of the Done class, which facilitates the toggling of a task's
	 * status to "done", before setting an output of ArrayList
	 * <String> describing the changes made to that specific task.
	 * 
	 * @return output: ArrayList<String>
	 */
	public ArrayList<String> run() {
		obtainTaskId();
		boolean isChanged = false;
		isChanged = changeStatus();
		if (isChanged) {
			saveToFile();
			createOutput();
		} else {
			createErrorOutput(MESSAGE_MARK_DONE_ERROR);
		}
		return output;
	}

	private void createOutput() {
		String text = String.format(MESSAGE_DONE, taskName);
		System.out.println(text);
		output.add(text);
	}

	protected boolean changeStatus() {
		for (int i = 0; i < taskList.size(); i++) {
			TaskObject task = taskList.get(i);
			if (task.getTaskId() == taskIdToMark) {
				originalTask.setTaskObject(task);
				originalTimings.addAll(task.getTaskDateTimes());

				taskName = task.getTitle();
				statusBeforeChange = task.getStatus();
				markedTask = task;

				if (task.getIsRecurring()) {
					changeStatusForRecurringTask(task);
				} else {
					task.setStatus(STATUS_COMPLETED);
				}

				originalTask.setTaskDateTimes(originalTimings);
				return true;
			}
		}
		return false;
	}

	// On top of this, consider implementation for event
	private void changeStatusForRecurringTask(TaskObject task) {
		if (task.getCategory().equals(CATEGORY_DEADLINE)) {
			changeStatusForRecurringDeadline(task);
		} else {
			if (task.getCategory().equals(CATEGORY_EVENT)) {
				changeStatusForRecurringEvent(task);
			}
		}
	}

	private void changeStatusForRecurringDeadline(TaskObject task) {
		try {
			Recurring.updateDeadline(task, taskList, STATUS_COMPLETED);
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			createErrorOutput(exceptionMessage);
		}
	}

	private void changeStatusForRecurringEvent(TaskObject task) {
		try {
			Recurring.updateEvent(task, taskList, STATUS_COMPLETED);
		} catch (RecurrenceException e) {
			String exceptionMessage = e.getRecurrenceExceptionMessage();
			createErrorOutput(exceptionMessage);
		}
	}

}
