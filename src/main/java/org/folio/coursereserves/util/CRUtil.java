package org.folio.coursereserves.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.folio.rest.impl.CourseAPI.COURSE_LISTINGS_TABLE;
import static org.folio.rest.impl.CourseAPI.TERMS_TABLE;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.Courselisting;
import org.folio.rest.jaxrs.model.CourseListingObject;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.TermObject;
import static org.folio.rest.persist.PgUtil.postgresClient;
import org.folio.rest.persist.PostgresClient;

public class CRUtil {
  public static Future<Courselisting> getExpandedCourseListing(String courseListingId,
      Map<String, String> okapiHeaders, Context context, Boolean expandTerm) {
    Future<Courselisting> future = Future.future();

    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(COURSE_LISTINGS_TABLE, courseListingId, Courselisting.class,
        reply -> {
      if(reply.failed()) {
        future.fail(reply.cause());
      } else if(reply.result() == null) {
        future.complete(null);
      } else {
        Courselisting result = reply.result();
        if(expandTerm == Boolean.TRUE) {
          String termId = result.getTermId();
          getExpandedTerm(termId, okapiHeaders, context).setHandler(reply2 -> {
            if(reply2.failed()) {
              future.fail(reply2.cause());
            } else {
              Term term = reply2.result();
              TermObject termObject = new TermObject();
              termObject.setEndDate(term.getEndDate());
              termObject.setStartDate(term.getStartDate());
              termObject.setId(term.getId());
              termObject.setName(term.getName());
              result.setTermObject(termObject);
              future.complete(result);
            }
          });
          future.complete(result);
        } else {
          future.complete(result);
        }
      }
    });
    return future;
  }

  public static Future<Term> getExpandedTerm(String termId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Term> future = Future.future();
    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(TERMS_TABLE, termId, Term.class,
        reply -> {
      if(reply.failed()) {
        future.fail(reply.cause());
      } else if(reply.result() == null) {
        future.complete(null);
      } else {
        Term result = reply.result();
        future.complete(result);
      }
    });
    return future;

  }

  public static Future<List<Course>> expandListOfCourses(List<Course> listOfCourses,
      Map<String, String> okapiHeaders, Context context) {
    Future<List<Course>> future = Future.future();
    List<Future> expandedCLFutureList = new ArrayList<>();
    for(Course course : listOfCourses) {
      expandedCLFutureList.add(getExpandedCourseListing(course.getCourseListingId(),
          okapiHeaders, context, Boolean.TRUE));
    }
    CompositeFuture compositeFuture = CompositeFuture.all(expandedCLFutureList);
    compositeFuture.setHandler(res -> {
      if(res.failed()) {
        future.fail(res.cause());
      } else {
        List<Course> newListOfCourses = new ArrayList<>();
        for( int i=0 ; i < expandedCLFutureList.size(); i++ ) {
          Future<Courselisting> f = (Future<Courselisting>)expandedCLFutureList.get(i);
          Courselisting courseListing = f.result();
          Course course = listOfCourses.get(i);
          CourseListingObject expandedCourseListing = new CourseListingObject();
          expandedCourseListing.setCourseTypeId(courseListing.getCourseTypeId());
          expandedCourseListing.setExternalId(courseListing.getExternalId());
          expandedCourseListing.setId(courseListing.getId());
          expandedCourseListing.setLocationId(courseListing.getLocationId());
          expandedCourseListing.setRegistrarId(courseListing.getRegistrarId());
          expandedCourseListing.setServicepointId(courseListing.getServicepointId());
          expandedCourseListing.setTermId(courseListing.getTermId());
          course.setCourseListingObject(expandedCourseListing);
          newListOfCourses.add(course);
        }
        future.complete(newListOfCourses);
      }
    });
    return future;
  }
}
