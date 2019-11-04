package org.folio.coursereserves.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.folio.rest.impl.CourseAPI.COURSE_LISTINGS_TABLE;
import static org.folio.rest.impl.CourseAPI.COURSE_TYPES_TABLE;
import static org.folio.rest.impl.CourseAPI.DEPARTMENTS_TABLE;
import static org.folio.rest.impl.CourseAPI.TERMS_TABLE;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.Courselisting;
import org.folio.rest.jaxrs.model.CourseListingObject;
import org.folio.rest.jaxrs.model.CourseTypeObject;
import org.folio.rest.jaxrs.model.Coursetype;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.DepartmentObject;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.TermObject;
import static org.folio.rest.persist.PgUtil.postgresClient;
import org.folio.rest.persist.PostgresClient;

public class CRUtil {
  public static Future<Courselisting> lookupExpandedCourseListing(String courseListingId,
      Map<String, String> okapiHeaders, Context context, Boolean expandTerm) {
    Future<Courselisting> future = Future.future();

    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(COURSE_LISTINGS_TABLE, courseListingId, Courselisting.class,
        courseListingReply -> {
      if(courseListingReply.failed()) {
        future.fail(courseListingReply.cause());
      } else if(courseListingReply.result() == null) {
        future.complete(null);
      } else {
        Courselisting result = courseListingReply.result();
        if(expandTerm == Boolean.TRUE) {
          String termId = result.getTermId();
          Future<Term> termFuture;
          if(termId == null) {
            termFuture = Future.succeededFuture();
          } else {
            termFuture = lookupTerm(termId, okapiHeaders, context);
          }
          termFuture.setHandler(termReply -> {
            if(termReply.failed()) {
              future.fail(termReply.cause());
            } else {
              Term term = termReply.result();
              if(term != null) {
                TermObject termObject = new TermObject();
                termObject.setEndDate(term.getEndDate());
                termObject.setStartDate(term.getStartDate());
                termObject.setId(term.getId());
                termObject.setName(term.getName());
                result.setTermObject(termObject);
              }
              String courseTypeId = result.getCourseTypeId();
              Future<Coursetype> courseTypeFuture;
              if(courseTypeId == null) {
                courseTypeFuture = Future.succeededFuture();
              } else {
                courseTypeFuture = lookupCourseType(result.getCourseTypeId(),
                    okapiHeaders, context);
              }
              courseTypeFuture.setHandler(courseTypeReply -> {
                if(courseTypeReply.failed()) {
                  future.fail(courseTypeReply.cause());
                } else {
                  if(courseTypeReply.result() != null) {
                    CourseTypeObject courseTypeObject = new CourseTypeObject();
                    Coursetype coursetype = courseTypeReply.result();
                    courseTypeObject.setId(coursetype.getId());
                    courseTypeObject.setDescription(coursetype.getDescription());
                    courseTypeObject.setName(coursetype.getName());
                    result.setCourseTypeObject(courseTypeObject);
                  }
                  future.complete(result);
                }
              });
            }
          });
        } else {
          future.complete(result);
        }
      }
    });
    return future;
  }

  public static Future<Term> lookupTerm(String termId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Term> future = Future.future();
    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(TERMS_TABLE, termId, Term.class,
        reply -> {
      if(reply.failed()) {
        future.fail(reply.cause());
      } else if(reply.result() == null) {
        future.complete();
      } else {
        Term result = reply.result();
        future.complete(result);
      }
    });
    return future;
  }

    public static Future<Department> lookupDepartment(String departmentId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Department> future = Future.future();
    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(DEPARTMENTS_TABLE, departmentId, Department.class,
        reply -> {
      if(reply.failed()) {
        future.fail(reply.cause());
      } else if(reply.result() == null) {
        future.complete(null);
      } else {
        Department result = reply.result();
        future.complete(result);
      }
    });
    return future;
  }

  public static Future<Coursetype> lookupCourseType(String courseTypeId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Coursetype> future = Future.future();
    PostgresClient postgresClient = postgresClient(context, okapiHeaders);
    postgresClient.getById(COURSE_TYPES_TABLE, courseTypeId, Coursetype.class,
        reply -> {
      if(reply.failed()) {
        future.fail(reply.cause());
      } else if(reply.result() == null) {
        future.complete(null);
      } else {
        future.complete(reply.result());
      }
    });
    return future;
  }

  /*
  public static Future<List<Course>> expandListOfCourses(List<Course> listOfCourses,
      Map<String, String> okapiHeaders, Context context) {
    Future<List<Course>> future = Future.future();
    List<Future> expandedCLFutureList = new ArrayList<>();
    for(Course course : listOfCourses) {
      expandedCLFutureList.add(lookupExpandedCourseListing(course.getCourseListingId(),
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
  */

  public static Future<List<Course>> expandListOfCourses(List<Course> listOfCourses,
      Map<String, String> okapiHeaders, Context context) {
    Future<List<Course>> future = Future.future();
    List<Future> expandedCourseFutureList = new ArrayList<>();
    for(Course course : listOfCourses) {
      expandedCourseFutureList.add(getExpandedCourse(course, okapiHeaders, context));
    }
    CompositeFuture compositeFuture = CompositeFuture.all(expandedCourseFutureList);
    compositeFuture.setHandler(expandCoursesRes -> {
      if(expandCoursesRes.failed()) {
        future.fail(expandCoursesRes.cause());
      } else {
        List<Course> newListOfCourses = new ArrayList<>();
        for( Future fut : expandedCourseFutureList ) {
          Future<Course> f = (Future<Course>)fut;
          newListOfCourses.add(f.result());
        }
        future.complete(newListOfCourses);
      }
    });
    return future;
  }


  public static Future<Course> getExpandedCourse(Course course,
      Map<String, String> okapiHeaders, Context context) {
    Future<Course> future = Future.future();
    Future<Courselisting> courseListingFuture;
    Course newCourse = copyCourse(course);
    if(course.getCourseListingId() == null) {
      courseListingFuture = Future.succeededFuture();
    } else {
      courseListingFuture = lookupExpandedCourseListing(course.getCourseListingId(),
          okapiHeaders, context, Boolean.TRUE);
    }
    courseListingFuture.setHandler(courselistingReply -> {
      if(courselistingReply.failed()) {
        future.fail(courselistingReply.cause());
      } else {
        CourseListingObject expandedCourseListing = new CourseListingObject();
        Courselisting courseListing = courselistingReply.result();
        if(courseListing != null) {
          expandedCourseListing.setCourseTypeId(courseListing.getCourseTypeId());
          expandedCourseListing.setExternalId(courseListing.getExternalId());
          expandedCourseListing.setId(courseListing.getId());
          expandedCourseListing.setLocationId(courseListing.getLocationId());
          expandedCourseListing.setRegistrarId(courseListing.getRegistrarId());
          expandedCourseListing.setServicepointId(courseListing.getServicepointId());
          expandedCourseListing.setTermId(courseListing.getTermId());
          expandedCourseListing.setTermObject(courseListing.getTermObject());
        }
        newCourse.setCourseListingObject(expandedCourseListing);

        Future<Department> departmentFuture;
        if(course.getDepartmentId() == null) {
          departmentFuture = Future.succeededFuture();
        } else {
          departmentFuture = lookupDepartment(course.getDepartmentId(), okapiHeaders,
              context);
        }
        departmentFuture.setHandler(departmentReply -> {
          if(departmentReply.failed()) {
            future.fail(departmentReply.cause());
          } else {
            Department department = departmentReply.result();
            if(department != null) {
              DepartmentObject departmentObject = new DepartmentObject();
              departmentObject.setId(department.getId());
              departmentObject.setName(department.getName());
              departmentObject.setDescription(department.getDescription());
              newCourse.setDepartmentObject(departmentObject);
            }
            future.complete(newCourse);
          }
        });

      }
    });
    return future;
  }

  private static Course copyCourse(Course originalCourse) {
    Course newCourse = new Course();
    newCourse.setId(originalCourse.getId());
    newCourse.setCourseListingId(originalCourse.getCourseListingId());
    newCourse.setCourseListingObject(originalCourse.getCourseListingObject());
    newCourse.setCourseNumber(originalCourse.getCourseNumber());
    newCourse.setDepartmentId(originalCourse.getDepartmentId());
    newCourse.setDepartmentObject(newCourse.getDepartmentObject());
    newCourse.setDescription(originalCourse.getDescription());
    newCourse.setSectionName(originalCourse.getSectionName());
    newCourse.setName(originalCourse.getName());
    return newCourse;
  }
}
