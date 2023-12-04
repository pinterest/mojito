package com.box.l10n.mojito.service.sla;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.doReturn;

import com.box.l10n.mojito.utils.DateTimeUtils;
import java.util.Arrays;
import java.time.ZonedDateTime;
import java.time.ZoneId;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.MockitoJUnitRunner;

/** @author jeanaurambault */
@RunWith(MockitoJUnitRunner.class)
public class DropScheduleServiceTest {

  @InjectMocks DropScheduleService dropSchedule;

  @Mock DateTimeUtils dateTimeUtils;

  @Spy DropScheduleConfig dropScheduleConfig;

  // TODO(jean) JSR310 - update
  ZoneId dateTimeZone = ZoneId.forID("PST8PDT");

  @Test
  public void testGetLastDropCreatedDate() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime now = new ZonedDateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
    doReturn(now).when(dateTimeUtils).now(dateTimeZone);
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-06-06T20:00:00.000-07:00", dateTimeZone);
    ZonedDateTime result = dropSchedule.getLastDropCreatedDate();
    assertEquals(expResult, result);
  }

  @Test
  public void testGetLastDropCreatedDatePreviousWeek() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime now = new ZonedDateTime("2018-06-05T14:00:00.000-07:00", dateTimeZone);
    doReturn(now).when(dateTimeUtils).now(dateTimeZone);
    // TODO(jean) JSR310 - replace
    ZonedDateTime expResult = new ZonedDateTime("2018-06-01T20:00:00.000-07:00", dateTimeZone);
    ZonedDateTime result = dropSchedule.getLastDropCreatedDate();
    assertEquals(expResult, result);
  }

  @Test
  public void testGetLastDropDueDateDuringWeekend() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-09T21:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateExactSameTime() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateSameDayBeforeDropTime() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T11:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-07T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateSameDayAfterDropTime() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T16:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-08T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateEmptyWorkingDays() {
    doReturn(Arrays.asList()).when(dropScheduleConfig).getDueDays();
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T16:00:00.000-07:00", dateTimeZone);
    assertNull(dropSchedule.getLastDropDueDate(before));
  }

  @Test
  public void testGetLastDropDueDatePreviousWeek() {
    doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-06T16:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetLastDropDueDateOneWeekAgo() {
    doReturn(Arrays.asList(5)).when(dropScheduleConfig).getDueDays();
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T11:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-01T14:00:00.000-07:00", dropSchedule.getLastDropDueDate(before).toString());
  }

  @Test
  public void testGetDropCreatedDate() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-08T14:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-06T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
  }

  @Test
  public void testGetDropCreatedDatePreviousWeek() {
    // TODO(jean) JSR310 - replace
    ZonedDateTime before = new ZonedDateTime("2018-06-05T14:00:00.000-07:00", dateTimeZone);
    assertEquals(
        "2018-06-01T20:00:00.000-07:00", dropSchedule.getDropCreatedDate(before).toString());
  }
}
