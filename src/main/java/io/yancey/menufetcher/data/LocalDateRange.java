package io.yancey.menufetcher.data;

import java.time.*;
import java.util.*;

public class LocalDateRange implements Iterable<LocalDate> {
  public final LocalDate startDate;
  public final LocalDate endDate;

  public LocalDateRange(LocalDate startDate, LocalDate endDate) {
    this.startDate = startDate;
    this.endDate = endDate;
  }

  public String toString() {
    return startDate + " - " + endDate;
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
  public boolean isBefore(LocalDate other) {
    return endDate.isBefore(other);
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
  public boolean isAfter(LocalDate other) {
    return startDate.isAfter(other);
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
  public boolean contains(LocalDate other) {
    return !isBefore(other) && !isAfter(other);
  }

  public boolean equals(Object o) {
    return o instanceof LocalDateRange &&
        startDate.equals(((LocalDateRange)o).startDate) &&
        endDate.equals(((LocalDateRange)o).endDate);
  }

  public int hashCode() {
    return startDate.hashCode() ^ endDate.hashCode();
  }

  @Override
  public Iterator<LocalDate> iterator() {
    return new Iterator<LocalDate>() {
      LocalDate date = startDate;

      @Override
      public LocalDate next() {
        return date = date.plusDays(1);
      }

      @Override
      public boolean hasNext() {
        return !date.equals(endDate);
      }
    };
  }
}
