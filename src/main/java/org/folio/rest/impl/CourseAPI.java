/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.core.Response;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.Courses;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.Section;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.tools.utils.ValidationHelper;

public class CourseAPI implements org.folio.rest.jaxrs.resource.Courses {
  
  public static final Logger logger = LoggerFactory.getLogger(
          CourseAPI.class);
  
  public static final String COURSES_TABLE = "courses_courses";
  public static final String SECTIONS_TABLE = "courses_sections";
  public static final String RESERVES_TABLE = "courses_reserves";
  public static final String INSTRUCTORS_TABLE = "courses_instructors";
  public static final String ROLES_TABLE = "courses_roles";
  public static final String SCHEDULES_TABLE = "courses_schedules";
  public static final String LOCATION_PREFIX = "/courses/";
  public static final String COURSES_PREFIX = LOCATION_PREFIX + "/courses";
  public static final String ROLES_PREFIX = LOCATION_PREFIX + "/roles";
  public static final String RESERVES_PREFIX = LOCATION_PREFIX + "/reserves";
  public static final String INSTRUCTORS_PREFIX = LOCATION_PREFIX + "/instructors";
  public static final String SCHEDULES_PREFIX = LOCATION_PREFIX + "/schedules";
  public static final String ID_FIELD = "'id'";
  
  public static boolean SUPPRESS_ERRORS = false;
  
  PostgresClient getPGClient(Context vertxContext, String tenantId) {
    return PostgresClient.getInstance(vertxContext.owner(), tenantId);
  }

  private String getErrorResponse(String response) {
    if(!SUPPRESS_ERRORS) {
      return response;
    }
    return "An error occurred";
  }

  private String logAndSaveError(Throwable err) {
    String message = err.getLocalizedMessage();
    logger.error(message, err);
    return message;
  }

  private String getTenant(Map<String, String> headers)  {
    return TenantTool.calculateTenantId(headers.get(
            RestVerticle.OKAPI_HEADER_TENANT));
  }

  private CQLWrapper getCQL(String query, int limit, int offset,
          String tableName) throws FieldException {
    CQL2PgJSON cql2pgJson = new CQL2PgJSON(tableName + ".jsonb");
    return new CQLWrapper(cql2pgJson, query).setLimit(new Limit(limit))
            .setOffset(new Offset(offset));
  }

  private boolean isDuplicate(String errorMessage){
    if(errorMessage != null && errorMessage.contains(
            "duplicate key value violates unique constraint")){
      return true;
    }
    return false;
  }

  private boolean isCQLError(Throwable err) {
    if(err.getCause() != null && err.getCause().getClass().getSimpleName()
            .endsWith("CQLParseException")) {
      return true;
    }
    return false;
  }

  @Override
  public void getCoursesCourses(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      PostgresClient pgClient = getPGClient(vertxContext, getTenant(okapiHeaders));
      CQLWrapper cql = getCQL(query, limit, offset, COURSES_TABLE);
      pgClient.get(COURSES_TABLE, Course.class, new String[]{"*"},
                cql, true, true, getReply -> {
          if(getReply.failed()) {
            String message = logAndSaveError(getReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                GetCoursesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            Courses courses = new Courses();
            List<Course> courseList = getReply.result().getResults();
            courses.setCourses(courseList);
            courses.setTotalRecords(getReply.result().getResultInfo()
                .getTotalRecords());
            asyncResultHandler.handle(Future.succeededFuture(
                GetCoursesCoursesResponse.respond200WithApplicationJson(courses)));
          }
        });
    } catch(Exception e) {
      String message = logAndSaveError(e);
      if(isCQLError(e)) {
        message = String.format("CQL Error: %s", message);
      }
      asyncResultHandler.handle(Future.succeededFuture(
          GetCoursesCoursesResponse.respond500WithTextPlain(
              getErrorResponse(message))));
    }
  }

  @Override
  public void postCoursesCourses(String lang, Course entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {     
      PostgresClient pgClient = getPGClient(vertxContext, getTenant(okapiHeaders));
      String id = entity.getId();
        if(id == null) {
          id = UUID.randomUUID().toString();
          entity.setId(id);
        }
      pgClient.save(COURSES_TABLE, id, entity, saveReply -> {
        if(saveReply.failed()) {
          String message = logAndSaveError(saveReply.cause());
          if(isDuplicate(message)) {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostCoursesCoursesResponse.respond422WithApplicationJson(
                    ValidationHelper.createValidationErrorMessage("name",
                    entity.getName(), "Course Exists"))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    PostCoursesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          }
        } else {
          String ret = saveReply.result();
          entity.setId(ret);
          asyncResultHandler.handle(Future.succeededFuture(
              PostCoursesCoursesResponse
                  .respond201WithApplicationJson(entity,
                      PostCoursesCoursesResponse.headersFor201()
                          .withLocation((COURSES_PREFIX + ret)))));
        }
      });
    } catch(Exception e) {
      String message = logAndSaveError(e);
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursesCoursesResponse.respond500WithTextPlain(
              getErrorResponse(message))));
    }
  }

  @Override
  public void deleteCoursesCourses(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesByCourseId(String courseId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursesCoursesByCourseId(String courseId, String lang, Course entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesByCourseId(String courseId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsByCourseId(String courseId, String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursesCoursesSectionsByCourseId(String courseId, String lang, Section entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsByCourseId(String courseId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsByCourseIdAndSectionId(String courseId, String sectionId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursesCoursesSectionsByCourseIdAndSectionId(String courseId, String sectionId, String lang, Section entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsByCourseIdAndSectionId(String courseId, String sectionId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(String courseId, String sectionId, String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(String courseId, String sectionId, String lang, Instructor entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(String courseId, String sectionId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(String courseId, String sectionId, String instructorId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(String courseId, String sectionId, String instructorId, String lang, Instructor entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(String courseId, String sectionId, String instructorId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsReservesByCourseIdAndSectionId(String courseId, String sectionId, String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursesCoursesSectionsReservesByCourseIdAndSectionId(String courseId, String sectionId, String lang, Section entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsReservesByCourseIdAndSectionId(String courseId, String sectionId, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(String courseId, String sectionId, String reserveId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(String courseId, String sectionId, String reserveId, String lang, Reserve entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(String courseId, String sectionId, String reserveId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

}
