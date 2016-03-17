package parser;


import java.util.ArrayList;
import java.util.List;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

import parser.Constants.TaskType;

/*
 * so far 
 * 1. from DT to DT
 * 2. by/before/at/on DT
 * 3. DT -> ddmmyyy, ddmonthyyyy, hhmm(hrs), hhmm(am/pm)
 * 
 * 1. (tmr/tomorrow) (get today date and + 1)
 * 
 * 2. (next/previous){x} (mon/tues/wed/thur/fri/sat/sun)(week)(year)(month) 
 * 		--> count number of next n prev 
 * 		--> figure out if its day, month or year
 * 		--> get current date/time
 * 		--> add the increment
 * 
 * 3. [\\d](mins)(hours)(days)(month)(year) (later/after/before/from now)
 * 		--> identify later/fromnow n etc, 
 * 		--> identify (mins or hour) (days, month or year)
 * 		--> convert \\d to integer
 * 		--> get current time or date
 * 		--> add the number of *unit* to current -> return 
 * 
 * 
 */

public class DateTimeParser {
	
	private int _startTime = -1;
	private int _endTime = -1;
	private int _startDate = -1;
	private int _endDate = -1;
	
	private String startT;
	private String endT;
	private String startD;
	private String endD;
	
	private LocalDate startDate = null;
	private LocalDate endDate = null;
	private LocalTime startTime = null;
	private LocalTime endTime = null;
	
	private LocalDateTime startDateTime = null;
	private LocalDateTime endDateTime = null;
	
	
	List<String> dtlist = new ArrayList<String>();
	
	private FileHandler fh = null;
	
	private static Logger logger = Logger.getLogger("DateTimeParser");
	
	/**
	 * method will take in string containing date and time, then splitting it into the date and time
	 * separately for parsing through dateparser and timeparser
	 * 
	 * @param input      user input in string format
	 * @param isForAdd   boolean to indicate if command is for the add parser
	 */
	public void parseDateTime(String input, boolean isForAdd) {
		if (isForAdd) {
			parseDateTimeForAdd(input);
		} else {
			TaskType tasktype = getTaskType(input);
			//separate stuff for different task types
			switch(tasktype) {
			case event:
				//find the "to" word n split
				for (String temp : input.split("to")) {
					dtlist.add(temp);
				}
				separateDateTime(dtlist.get(0), true);
				separateDateTime(dtlist.get(1), false);
				break;
			case deadline:
			default:
				separateDateTime(input, true);
				break;
			}
			setLocalDateTime(isForAdd, tasktype);
		}
	}
	
	
	/**
	 * method will parse date time string according to its task type
	 * 
	 * @param input   user's input in a string format
	 */
	private void parseDateTimeForAdd(String input) {
		TaskType tasktype = getTaskType(input);
		//separate stuff for different task types
		switch(tasktype) {
		case event:
			//find the "to" word n split
			for (String temp : input.split("to")) {
				dtlist.add(temp);
			}
			separateDateTime(dtlist.get(0), true);
			separateDateTime(dtlist.get(1), false);
			break;
		case deadline:
			separateDateTime(input, true);
			break;
		default:
			break;
		}
		setLocalDateTime(true, tasktype);
	}
	
	/**
	 * method will take in string and identify regular expressions for time
	 * and date. It will create date processor object and tie processor object
	 * to parse date and time
	 * 
	 * @param input
	 * @param isStart
	 */
	public void separateDateTime(String input, boolean isStart) {
		//Pattern date = Pattern.compile(Constants.REGEX_DATE_FORMAT);
		Pattern time = Pattern.compile(Constants.REGEX_TIME_FORMAT);
		//Pattern relativedate = Pattern.compile(Constants.REGEX_RELATIVE_DATE_ALL);
		
		//Matcher dateMatcher = date.matcher(input);
		Matcher timeMatcher = time.matcher(input);
		//Matcher rdateMatcher = relativedate.matcher(input);
		
		DateParser DP = new DateParser();
		TimeParser TP = new TimeParser();
		
		String _date = "", _time = "";
		
		if (timeMatcher.find()) {
			logger.log(Level.INFO, "Time format found");
			_time = getTrimmedString(input, timeMatcher.start(), timeMatcher.end());
			_date = input.replaceAll(_time, "").trim();
		} else {
			logger.log(Level.INFO, "Time format NOT found");
			_date = input;
		}
		
		processParallel(DP, TP, _date, _time);
		
		if (isStart) {
			_startTime = TP.getTime();
			_startDate = DP.getStartDate();
			startT = TP.getTimeString();
			startD = DP.getDateString();
			startTime = TP.getTimeObject();
			startDate = DP.getDateObject();
		} else {
			_endTime = TP.getTime();
			_endDate = DP.getStartDate();
			endT = TP.getTimeString();
			endD = DP.getDateString();
			endTime = TP.getTimeObject();
			endDate = DP.getDateObject();
		}
	}

	
	public void processParallel(DateParser DP, TimeParser TP, String _date,
			String _time) {
		_time = cleanString(_time);
		_date = cleanString(_date);
		DP.processDate(_date);
		TP.processTime(_time);
	}
	
	/**
	 * 
	 */
	public void setLocalDateTime(boolean isForAdd, TaskType task) {
		DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("uuuu-MM-dd");
		DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");
		
		if (isForAdd) {
			if (task.toString() == "event" && _endDate == -1) { 
				_endDate = _startDate;
				endD = startD; //for special case of lazy ppl not typing end date
				endDate = startDate;
			}
			if(task.toString() == "event") {
				endDateTime = LocalDateTime.of(endDate, endTime);
			}
		}
		startDateTime = LocalDateTime.of(startDate, startTime);
	}
	
	/**
	 * method checks string to identify the task type
	 * 
	 * @param input    user's input in string format
	 * @return         appropriate task type for the input 
	 */
	public TaskType getTaskType(String input) {
		if (input.matches(Constants.REGEX_DEADLINE_IDENTIFIER)) {
			logger.log(Level.INFO, "Deadline recognised");
			return TaskType.deadline;
		} else if (input.matches(Constants.REGEX_EVENT_IDENTIFIER)) {
			logger.log(Level.INFO, "Event recognised");
			return TaskType.event;
		} else if (input.matches(Constants.REGEX_POINT_TASK_IDENTIFIER)) {
			logger.log(Level.INFO, "Event recognised");
			return TaskType.deadline;
		} else {
			logger.log(Level.INFO, "Floating recognised");
			return TaskType.floating;
		}
	}
	
	
	
	//nid to take note of "7 days from now" kind of query, dont remove from, or recognise now
	private String cleanString(String input) {
		return input.replaceAll(Constants.REGEX_TASK_IDENTIFIER, "").trim();
	}
	
	//extract string and trims out whitespace
	private String getTrimmedString(String input, int startIndex, int endIndex) {
		return input.substring(startIndex, endIndex).trim();
	}
	
	public int getStartDate() {
		return _startDate;
	}
	
	public int getEndDate() {
		return _endDate;
	}
	
	public int getStartTime() {
		return _startTime;
	}
	
	public int getEndTime() {
		return _endTime;
	}
	
	public LocalDateTime getStartDateTime() {
		return startDateTime;
	}
	
	public LocalDateTime getEndDateTime() {
		return endDateTime;
	}
}
