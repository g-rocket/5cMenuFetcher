package io.yancey.menufetcher;

import java.time.*;

public class LocalTimeRange {
	public final LocalTime startTime;
	public final LocalTime endTime;
	
	public LocalTimeRange(LocalTime startTime, LocalTime endTime) {
		this.startTime = startTime;
		this.endTime = endTime;
	}
	
	public String toString() {
		return startTime + " - " + endTime;
	}
	
	/**
     * Checks if this range is entirely before the specified time.
     * <p>
     * The comparison is based on the time-line position of the time within a day.
     *
     * @param other  the other time to compare to, not null
     * @return true if this range is entirely before the specified time
     * @throws NullPointerException if {@code other} is null
     */
	public boolean isBefore(LocalTime other) {
		return endTime.isBefore(other);
	}
	
	/**
     * Checks if this range is entirely after the specified time.
     * <p>
     * The comparison is based on the time-line position of the time within a day.
     *
     * @param other  the other time to compare to, not null
     * @return true if this range is entirely after the specified time
     * @throws NullPointerException if {@code other} is null
     */
	public boolean isAfter(LocalTime other) {
		return startTime.isAfter(other);
	}
	
	/**
     * Checks if this range contains the specified time.
     * <p>
     * The comparison is based on the time-line position of the time within a day.
     *
     * @param other  the other time to compare to, not null
     * @return true if this range contains the specified time
     * @throws NullPointerException if {@code other} is null
     */
	public boolean contains(LocalTime other) {
		return !isBefore(other) && !isAfter(other);
	}
	
	public boolean equals(Object o) {
		return o instanceof LocalTimeRange &&
				startTime.equals(((LocalTimeRange)o).startTime) &&
				endTime.equals(((LocalTimeRange)o).endTime);
	}
	
	public int hashCode() {
		return startTime.hashCode() ^ endTime.hashCode();
	}
}
