package com.pulsar;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import net.sf.saxon.om.Axis;
import net.sf.saxon.om.AxisIterator;
import net.sf.saxon.om.Navigator;
import net.sf.saxon.tinytree.TinyNodeImpl;

public class AIXMTimesheetParser {
	
	private static ArrayList<String> weekDays = new ArrayList<String>(Arrays.asList("MON","TUE","WED","THU","FRI","SAT","SUN"));

	public static boolean dummy(List<TinyNodeImpl> element) {
		boolean flag = true;
		System.out.println("#"+((TinyNodeImpl)element.get(0)).getDisplayName()+"#");
		AxisIterator it = ((TinyNodeImpl)element.get(0)).iterateAxis(Axis.DESCENDANT);
		while (it.next() != null) {
			System.out.println("%"+((TinyNodeImpl)it.current()).getDisplayName()+"%");
			String att = Navigator.getAttributeValue(((TinyNodeImpl)it.current()), "http://www.w3.org/2001/XMLSchema-instance", "nil");
			if (att != null) {
				System.out.println(att);
			}
			System.out.println(((TinyNodeImpl)it.current()).getStringValue().trim());
		}
		flag = true;
//		if (element == null || element.length()==0) {
//			flag = false;
//		} else if (element.contains("aixm:timeSLice")) {
//			flag = true;
//		} else return false;
		return flag;
	}
	
	public static boolean timeSheetHasOccurrencesInTimeSlice(TinyNodeImpl timeSheet, TinyNodeImpl validTime) {
		boolean flag = true;
		String rangeStart = "";
		String rangeEnd = "";
		AxisIterator it = validTime.iterateAxis(Axis.DESCENDANT);
		while (it.next() != null) {
			System.out.println("%"+((TinyNodeImpl)it.current()).getDisplayName()+"%");
			if (((TinyNodeImpl)it.current()).getDisplayName().equals("gml:beginPosition")) {
				rangeStart = ((TinyNodeImpl)it.current()).getStringValue();
			} else if (((TinyNodeImpl)it.current()).getDisplayName().equals("gml:endPosition")) {
				rangeEnd = ((TinyNodeImpl)it.current()).getStringValue();
			}
		}

		
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.valueOf(rangeStart.substring(0,4)), Integer.valueOf(rangeStart.substring(4,6)), Integer.valueOf(rangeStart.substring(6,8)),
				Integer.valueOf(rangeStart.substring(9,11)), Integer.valueOf(rangeStart.substring(12,14)), Integer.valueOf(rangeStart.substring(15,17)));
		System.out.println(cal.toString());
		Date tsStart = cal.getTime();
		
		if (rangeEnd.equals("")) {
			return true;
		}
		
		cal.set(Integer.valueOf(rangeEnd.substring(0,4)), Integer.valueOf(rangeEnd.substring(4,6)), Integer.valueOf(rangeEnd.substring(6,8)),
				Integer.valueOf(rangeEnd.substring(9,11)), Integer.valueOf(rangeEnd.substring(12,14)), Integer.valueOf(rangeEnd.substring(15,17)));
		System.out.println(cal.toString());
		Date tsEnd = cal.getTime();
		
		cal.add(Calendar.YEAR, -1);
		Date temp = cal.getTime();
		//if (end-start > 1 year) then answer = yes
		if (cal.getTime().after(tsStart)) {
			//range is longer than one year => no need to check more
			return true;
		}
		
		String startDateStr = "";
		String endDateStr = "";
		it = timeSheet.iterateAxis(Axis.DESCENDANT);
		while (it.next() != null) {
			System.out.println("%"+((TinyNodeImpl)it.current()).getDisplayName()+"%");
			if (((TinyNodeImpl)it.current()).getDisplayName().equals("aixm:startDate")) {
				startDateStr = ((TinyNodeImpl)it.current()).getStringValue();
			} else if (((TinyNodeImpl)it.current()).getDisplayName().equals("aixm:endDate")) {
				endDateStr = ((TinyNodeImpl)it.current()).getStringValue();
			}
		}
		//else if (TS.end < start, same year) then answer = no
		//get month of endDate, compare to month of time slice start date
		if (endDateStr.length()>0) {
			cal.setTime(tsStart);
			if (Integer.parseInt(endDateStr.substring(3,5)) < cal.get(Calendar.MONTH)) {
				return false;
			} else if ((Integer.parseInt(endDateStr.substring(3,5)) == cal.get(Calendar.MONTH)) && (Integer.parseInt(endDateStr.substring(0,2)) < cal.get(Calendar.DAY_OF_MONTH))) {
				return false;
			}
		}
		
		//else if (TS.start > end, same year) then answer = no
		//get month of startDate, compare to month of time slice end date 
		if (startDateStr.length()>0) {
			cal.setTime(tsEnd);
			if (Integer.parseInt(startDateStr.substring(3,5)) > cal.get(Calendar.MONTH)) {
				return false;
			} else if ((Integer.parseInt(startDateStr.substring(3,5)) == cal.get(Calendar.MONTH)) && (Integer.parseInt(startDateStr.substring(0,2)) > cal.get(Calendar.DAY_OF_MONTH))) {
				return false;
			}
		}
		
		//else occurrence possible but no guarantee depending upon other timesheet specs
		
		//if day is specified and is a week day, and if the TimeSlice last less than a week, there's a chance day doesn't fit
		//i.e. it's dangerous to specify a day for a TimeSlice of less than a week
		
		long delta = (tsEnd.getTime() - tsStart.getTime())/86400/1000;
		if (delta < 7) {
			//TimeSlice is valid less than a week, there's a risk not to have any occurrence of day in range
			String day = "";
			it = timeSheet.iterateAxis(Axis.DESCENDANT);
			while (it.next() != null) {
				System.out.println("%"+((TinyNodeImpl)it.current()).getDisplayName()+"%");
				if (((TinyNodeImpl)it.current()).getDisplayName().equals("aixm:day")) {
					day = ((TinyNodeImpl)it.current()).getStringValue();
					break;
				}
			}
			
			if (weekDays.contains(day)) {
				return false;
			}
		}
		
		
		return flag;
	}
	
	public static boolean timesheetOccursOnDate(TinyNodeImpl timeSheet, String dateStr) {
		//if excluded = YES, timesheet cannot occur on dateStr
		String excluded = getElement(timeSheet, "aixm:excluded");
		if (excluded != null && excluded.equals("YES")) {
			return false;
		}
		
		//if timesheet starts after dateStr, timesheet cannot occur on dateStr
		String startDateStr = getElement(timeSheet, "aixm:startDate");
		if (startDateStr != null && startDateStr.equals("")) {
			//this is an assumption
			startDateStr = "01-01";
		}
		if (startDateStr != null && dateStr.substring(4,8).compareTo(startDateStr.substring(3,5) + startDateStr.substring(0,2)) < 0) {
			return false;
		}
		
		//if timesheet ends before dateStr, timesheet cannot occur on dateStr
		String endDateStr = getElement(timeSheet, "aixm:endDate");
		if (endDateStr != null && endDateStr.equals("")) {
			//this is an assumption
			endDateStr = "31-12";
		}
		if (endDateStr != null && dateStr.substring(4,8).compareTo(endDateStr.substring(3,5) + endDateStr.substring(0,2)) > 0) {
			return false;
		}
		
		//if day is in [MON-SUN] and doesn't math dateStr day, timesheet cannot occur on dateStr
		String day = getElement(timeSheet, "aixm:day");
		String dayTil = getElement(timeSheet, "aixm:dayTil"); //TODO take this into account
		if (day != null) {
			if (weekDays.contains(day)) {
				if (!dayOfDate(dateStr).equals(day)) {
					return false;
				}
			}
		}
		
		//TODO add additional checks to enhance detection...
		
		
		
		//at this point, we have no evidence yet timesheet cannot have an occurrence on dateStr
		return true;
	}
	
	public static boolean timesheetOccursInRange(TinyNodeImpl timeSheet, String startDateStr, String endDateStr) {
		if (!startDateStr.equals("") && !endDateStr.equals("")) {
			Date start = dateOfDateStr(startDateStr);
			Date end = dateOfDateStr(endDateStr);
			Date currentDate = start;
			while (currentDate.before(end)) {
				String currentDateStr = strOfDate(currentDate);
				if (timesheetOccursOnDate(timeSheet, currentDateStr)) {
					return true;
				}
				currentDate.setTime(currentDate.getTime() + 86400000); // add one day in milliseconds (86400 sec/day)
			}
			return false;
		} else {
			return false;
		}
	}
	
	public static boolean timesheetsOccurTogether(TinyNodeImpl timeSheet1, TinyNodeImpl timeSheet2, String startDateStr, String endDateStr) {
		if (!startDateStr.equals("") && !endDateStr.equals("")) {
			Date start = dateOfDateStr(startDateStr);
			Date end = dateOfDateStr(endDateStr);
			Date currentDate = start;
			while (currentDate.before(end)) {
				String currentDateStr = strOfDate(currentDate);
				if (timesheetOccursOnDate(timeSheet1, currentDateStr) && timesheetOccursOnDate(timeSheet2, currentDateStr)) {
					return true;
				}
				currentDate.setTime(currentDate.getTime() + 86400000); // add one day in milliseconds (86400 sec/day)
			}
			return false;
		} else {
			return false;
		}
	}
	
	public static boolean dayOccursInTimesheetRange(String day, String startDateStr, String endDateStr) {
		if (day.equals("ANY")) {
			return true; //any day occur in any range
		} else if (endDateStr.equals("")) {
			return true; //any day occur in unlimited period
		} else if (weekDays.contains(day) && lessThanAWeek(startDateStr, endDateStr)) {
			return false; //if range is less than a week, it's not sure day occurs in range
		} else {
			//in any other case, cannot conclude => return true to prevent false alarms
			return true;
		}
	}
	
	private static boolean lessThanAWeek(String start, String end) {
		Calendar cal = Calendar.getInstance();
		cal.set(Calendar.HOUR,0);
		cal.set(Calendar.MINUTE,0);
		cal.set(Calendar.SECOND,0);
		cal.set(Calendar.MILLISECOND,0);
		
		cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(start.substring(0,2)));
		cal.set(Calendar.MONTH, Integer.valueOf(start.substring(3,5)));
		Date startDate = cal.getTime();
		
		cal.set(Calendar.DAY_OF_MONTH, Integer.valueOf(end.substring(0,2)));
		cal.set(Calendar.MONTH, Integer.valueOf(end.substring(3,5)));
		Date endDate = cal.getTime();
		
		
		return (((endDate.getTime() - startDate.getTime())/86400) < 7000);
	}
	
	private static String getElement(TinyNodeImpl node, String elementName) {
		AxisIterator it = node.iterateAxis(Axis.DESCENDANT);
		String elementValue = null;
		while (it.next() != null) {
			System.out.println("%"+((TinyNodeImpl)it.current()).getDisplayName()+"%");
			if (((TinyNodeImpl)it.current()).getDisplayName().equals(elementName)) {
				elementValue = ((TinyNodeImpl)it.current()).getStringValue();
				break;
			}
		}
		return elementValue;
	}
	
	public static boolean isBefore(String date1, String date2) {

		String first = date1.substring(3,5)+date1.substring(0,2);
		String second = date2.substring(3,5)+date2.substring(0,2);
		return (first.compareTo(second)<0);
	}
	
	private static String dayOfDate(String date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(dateOfDateStr(date));
		
		String day = weekDays.get(cal.get(Calendar.DAY_OF_WEEK));
		return day;
		
	}
	
	private static Date dateOfDateStr(String date) {
		Calendar cal = Calendar.getInstance();
		cal.set(Integer.valueOf(date.substring(0,4)), Integer.valueOf(date.substring(4,6)), Integer.valueOf(date.substring(6,8)),
				Integer.valueOf(date.substring(9,11)), Integer.valueOf(date.substring(12,14)), Integer.valueOf(date.substring(15,17)));
		System.out.println(cal.toString());
		return cal.getTime();
	}
	
	private static String strOfDate(Date date) {
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
//		String format = String.format("%%0%dd", digits);
//		String result = String.format(format, num);

		String str = String.valueOf(cal.get(Calendar.YEAR)) + 
					String.format("%02d", cal.get(Calendar.MONTH)) +
					String.format("%02d",cal.get(Calendar.DAY_OF_MONTH)) +
					"T" +
					String.format("%02d",cal.get(Calendar.HOUR_OF_DAY)) + ":" +
					String.format("%02d",cal.get(Calendar.MINUTE)) + ":" +
					String.format("%02d",cal.get(Calendar.SECOND)) +
					"Z";
		return str;
		
	}
	
}
