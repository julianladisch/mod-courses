package org.folio.rest.impl;

import static org.junit.Assert.assertFalse;
import org.junit.Test;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Before;

public class CourseAPITest {

  @Before
  @After
  public void cleanUp() {
    CourseAPI.setSuppressErrors(false);
  }

  @Test
  public void testIsDuplicate() {
    String message = "oh no, the duplicate key value violates unique constraint";
    assertTrue(CourseAPI.isDuplicate(message));
  }

  @Test
  public void testGetErrorResponse() {
    String error = "The googleflarble blipped";
    CourseAPI.setSuppressErrors(true);
    assertTrue(CourseAPI.getErrorResponse(error).equals("An error occurred"));
  }

  @Test
  public void testIsCQLError() {
    Exception e = new Exception("Whatever");
    assertFalse(CourseAPI.isCQLError(e));
  }


}
