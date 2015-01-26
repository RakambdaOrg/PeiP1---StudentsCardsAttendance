package fr.tours.polytech.DI.RFID.objects;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import java.util.regex.Pattern;
import fr.tours.polytech.DI.RFID.utils.Configuration;
import fr.tours.polytech.DI.RFID.utils.Utils;

/**
 * Class representing a period for checking.
 *
 * @author COLEAU Victor, COUCHOUD Thomas
 */
public class Period
{
	private int startingHour;
	private int startingMinute;
	private int endingHour;
	private int endingMinute;
	private Calendar calendar;
	private DecimalFormat decimalFormat;

	/**
	 * Constructor.
	 *
	 * @param period A string representing the period. This should be formatted as <i>xx</i><b>h</b><i>xx</i><b>-</b><i>yy</i><b>h</b><i>yy</i> where <i>xx</i> and <i>yy</i> are the time to set.
	 *
	 * @throws IllegalArgumentException If the period isn't formatted as it should be.
	 */
	public Period(String period) throws IllegalArgumentException
	{
		if(!Pattern.matches("(\\d{1,2})(h|H)(\\d{1,2})(-)(\\d{1,2})(h|H)(\\d{1,2})", period))
			throw new IllegalArgumentException("Time should be formatted as xx:xx-yy:yy (was " + period + ")");
		period = period.toUpperCase().replaceAll(" ", "");
		String starting = period.substring(0, period.indexOf('-'));
		String ending = period.substring(period.indexOf('-') + 1);
		this.startingHour = Integer.parseInt(starting.substring(0, starting.indexOf("H")));
		this.startingMinute = Integer.parseInt(starting.substring(starting.indexOf("H") + 1));
		this.endingHour = Integer.parseInt(ending.substring(0, ending.indexOf("H")));
		this.endingMinute = Integer.parseInt(ending.substring(ending.indexOf("H") + 1));
		this.calendar = Calendar.getInstance();
		this.decimalFormat = new DecimalFormat("00");
	}

	/**
	 * Used to get all the periods from the config file.
	 *
	 * @return An array list of the periods.
	 */
	public static ArrayList<Period> loadPeriods()
	{
		ArrayList<Period> periods = new ArrayList<Period>();
		String[] stringPeriods = Utils.config.getConfigValue(Configuration.PERIODS).getStringArray();
		for(String stringPeriod : stringPeriods)
			try
			{
				periods.add(new Period(stringPeriod));
			}
			catch(Exception exception)
			{
				Utils.logger.log(Level.WARNING, "Can't load perriod " + stringPeriod, exception);
			}
		return periods;
	}

	/**
	 * Used to get a String representing this interval.
	 *
	 * @return A string formatted as <b>xxHxx - yyHyy</b>
	 */
	public String getTimeInterval()
	{
		return this.startingHour + "H" + this.decimalFormat.format(this.startingMinute) + " - " + this.endingHour + "H" + this.decimalFormat.format(this.endingMinute);
	}

	/**
	 * Used to know if the date is in this period.
	 *
	 * @param date The date to verify.
	 * @return true if the date is in the period, false if not.
	 */
	public boolean isInPeriod(Date date)
	{
		this.calendar.setTime(date);
		int hours = this.calendar.get(Calendar.HOUR);
		int minutes = this.calendar.get(Calendar.MINUTE);
		if(this.startingHour == this.endingHour)
		{
			if(hours == this.startingHour)
				if(minutes >= this.startingMinute && minutes < this.endingMinute)
					return true;
		}
		else if(hours >= this.startingHour && hours < this.endingHour)
			if(hours == this.startingHour)
			{
				if(minutes >= this.startingMinute)
					return true;
			}
			else if(hours == this.endingHour)
			{
				if(minutes < this.endingMinute)
					return true;
			}
			else
				return true;
		return false;
	}

	/**
	 * Used to know if two Period objects are overlapping.
	 *
	 * @param period The other Period to check with.
	 * @return true if overlapping, false if not.
	 */
	public boolean isOverlapped(Period period)
	{
		if(period == null)
			return false;
		if(period.isInPeriod(getStartingDate()) || period.isInPeriod(getEndingDate()) || isInPeriod(period.getStartingDate()) || isInPeriod(period.getEndingDate()))
			return true;
		return false;
	}

	/**
	 * Used to get a String representing this interval. This is mostly used when saving the object to the config, should use {@link getTimeInterval} instead.
	 *
	 * @return A string formatted as <b>xxHxx-yyHyy</b>
	 */
	@Override
	public String toString()
	{
		return getTimeInterval().replaceAll(" ", "");
	}

	/**
	 * Use to get the ending date of this period in the current day.
	 *
	 * @return The ending date.
	 */
	private Date getEndingDate()
	{
		Calendar calen = Calendar.getInstance();
		calen.setTime(new Date());
		calen.set(Calendar.HOUR, this.endingHour);
		calen.set(Calendar.MINUTE, this.endingMinute);
		return calen.getTime();
	}

	/**
	 * Use to get the starting date of this period in the current day.
	 *
	 * @return The starting date.
	 */
	private Date getStartingDate()
	{
		Calendar calen = Calendar.getInstance();
		calen.setTime(new Date());
		calen.set(Calendar.HOUR, this.startingHour);
		calen.set(Calendar.MINUTE, this.startingMinute);
		return calen.getTime();
	}
}
