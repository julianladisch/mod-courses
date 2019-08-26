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
import org.folio.rest.jaxrs.model.Reserves;
import org.folio.rest.jaxrs.model.Role;
import org.folio.rest.jaxrs.model.Schedule;
import org.folio.rest.jaxrs.model.Schedules;
import org.folio.rest.jaxrs.model.Section;
import org.folio.rest.jaxrs.model.Sections;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
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
  public void deleteCoursesCourses(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", COURSES_TABLE);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCoursesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesCoursesByCourseId(String courseId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(COURSES_TABLE, Course.class, courseId, okapiHeaders, vertxContext,
        GetCoursesCoursesByCourseIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursesCoursesByCourseId(String courseId, String lang,
      Course entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COURSES_TABLE, entity, courseId, okapiHeaders, vertxContext,
        PutCoursesCoursesByCourseIdResponse.class, asyncResultHandler);

  }

  @Override
  public void deleteCoursesCoursesByCourseId(String courseId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COURSES_TABLE, courseId, okapiHeaders, vertxContext, 
        DeleteCoursesCoursesByCourseIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursesCoursesSectionsByCourseId(String courseId, String query,
      int offset, int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String courseQueryClause = String.format("courseId = %s", courseId);
    if(query == null || query.isEmpty()) {
      query = courseQueryClause;
    } else {
      query = String.format("(%s) AND (%s)", query, courseQueryClause);
    }
    PgUtil.get(SECTIONS_TABLE, Section.class, Sections.class, query, offset, limit,
        okapiHeaders, vertxContext, GetCoursesCoursesSectionsByCourseIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursesCoursesSectionsByCourseId(String courseId, String lang,
      Section entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getCourseId() != courseId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursesCoursesSectionsByCourseIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("courseId", entity.getCourseId(), 
                      String.format("courseId should be %s", courseId)))));
    } else {
      PgUtil.post(SECTIONS_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursesCoursesSectionsByCourseIdResponse.class, asyncResultHandler);
    }
  }
    

  @Override
  public void deleteCoursesCoursesSectionsByCourseId(String courseId, Map<String,
      String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format(
            "DELETE FROM %s_%s.%s WHERE jsonb->>'courseId' = '%s'",
                tenantId, "mod_courses", SECTIONS_TABLE, courseId);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCoursesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesCoursesSectionsByCourseIdAndSectionId(String courseId, 
      String sectionId, String lang, Map<String, String> okapiHeaders, 
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(SECTIONS_TABLE, Section.class, sectionId, okapiHeaders, vertxContext,
        GetCoursesCoursesSectionsByCourseIdAndSectionIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursesCoursesSectionsByCourseIdAndSectionId(String courseId,
      String sectionId, String lang, Section entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(SECTIONS_TABLE, entity, sectionId, okapiHeaders, vertxContext,
        PutCoursesCoursesSectionsByCourseIdAndSectionIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesCoursesSectionsByCourseIdAndSectionId(String courseId, 
      String sectionId, String lang, Map<String, String> okapiHeaders, 
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(SECTIONS_TABLE, sectionId, okapiHeaders, vertxContext,
        DeleteCoursesCoursesSectionsByCourseIdAndSectionIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(
      String courseId, String sectionId, String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String sectionQueryClause = String.format("sectionId = %s", sectionId);
    if(query == null || query.isEmpty()) {
      query = sectionQueryClause;
    } else {
      query = String.format("(%s) AND (%s)", query, sectionQueryClause);
    }
    PgUtil.get(INSTRUCTORS_TABLE, Section.class, Sections.class, query, offset, limit,
        okapiHeaders, vertxContext,
        GetCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(
      String courseId, String sectionId, String lang, Instructor entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getSectionId() != sectionId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("sectionId", entity.getSectionId(), 
                      String.format("sectionId should be %s", sectionId)))));
    } else {
      PgUtil.post(INSTRUCTORS_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse.class,
          asyncResultHandler);
    }
  }

  @Override
  public void deleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionId(
      String courseId, String sectionId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format(
            "DELETE FROM %s_%s.%s WHERE jsonb->>'sectionId' = '%s'",
                tenantId, "mod_courses", INSTRUCTORS_TABLE, sectionId);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
                        .respond500WithTextPlain(getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
            DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
                .respond500WithTextPlain(getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(
      String courseId, String sectionId, String instructorId, String lang,
      Map<String, String> okapiHeaders, 
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INSTRUCTORS_TABLE, Instructor.class, instructorId, okapiHeaders,
        vertxContext, 
        GetCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(
      String courseId, String sectionId, String instructorId, String lang,
      Instructor entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INSTRUCTORS_TABLE, entity, instructorId, okapiHeaders, vertxContext,
        PutCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorId(
      String courseId, String sectionId, String instructorId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.deleteById(INSTRUCTORS_TABLE, instructorId, okapiHeaders, vertxContext,
        DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursesCoursesSectionsReservesByCourseIdAndSectionId(
      String courseId, String sectionId, String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String sectionQueryClause = String.format("sectionId = %s", sectionId);
    if(query == null || query.isEmpty()) {
      query = sectionQueryClause;
    } else {
      query = String.format("(%s) AND (%s)", sectionQueryClause, query);
    }
    PgUtil.get(RESERVES_TABLE, Reserve.class, Reserves.class, query, offset, limit,
        okapiHeaders, vertxContext,
        GetCoursesCoursesSectionsReservesByCourseIdAndSectionIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursesCoursesSectionsReservesByCourseIdAndSectionId(
      String courseId, String sectionId, String lang, Reserve entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getSectionId() != sectionId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("sectionId", entity.getSectionId(), 
                      String.format("sectionId should be %s", sectionId)))));
    } else {
      PgUtil.post(RESERVES_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse.class,
          asyncResultHandler);
    }
  }

  @Override
  public void deleteCoursesCoursesSectionsReservesByCourseIdAndSectionId(
      String courseId, String sectionId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format(
            "DELETE FROM %s_%s.%s WHERE jsonb->>'sectionId' = '%s'",
                tenantId, "mod_courses", RESERVES_TABLE, sectionId);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
                        .respond500WithTextPlain(getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
            DeleteCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdResponse
                .respond500WithTextPlain(getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(
      String courseId, String sectionId, String reserveId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(RESERVES_TABLE, Instructor.class, reserveId, okapiHeaders,
        vertxContext,
        GetCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(
      String courseId, String sectionId, String reserveId, String lang,
      Reserve entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(RESERVES_TABLE, entity, reserveId, okapiHeaders, vertxContext,
        PutCoursesCoursesSectionsInstructorsByCourseIdAndSectionIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveId(
      String courseId, String sectionId, String reserveId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.deleteById(RESERVES_TABLE, reserveId, okapiHeaders, vertxContext,
        DeleteCoursesCoursesSectionsReservesByCourseIdAndSectionIdAndReserveIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursesRoles(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(ROLES_TABLE, Section.class, Sections.class, query, offset, limit,
        okapiHeaders, vertxContext, GetCoursesRolesResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursesRoles(String lang, Role entity, 
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.post(ROLES_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursesRolesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesRoles(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", ROLES_TABLE);
        logger.info(String.format("Deleting all roles with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesRolesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesRolesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
            DeleteCoursesRolesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesRolesByRoleId(String roleId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(ROLES_TABLE, Role.class, roleId, okapiHeaders, vertxContext,
        GetCoursesRolesByRoleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursesRolesByRoleId(String roleId, String lang, Role entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.put(ROLES_TABLE, entity, roleId, okapiHeaders, vertxContext,
        PutCoursesCoursesByCourseIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesRolesByRoleId(String roleId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.deleteById(ROLES_TABLE, roleId, okapiHeaders, vertxContext,
        DeleteCoursesRolesByRoleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursesSchedules(String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.get(SCHEDULES_TABLE, Schedule.class, Schedules.class, query, offset, limit,
        okapiHeaders, vertxContext, GetCoursesRolesResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursesSchedules(String lang, Schedule entity,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.post(SCHEDULES_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursesSchedulesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesSchedules(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", SCHEDULES_TABLE);
        logger.info(String.format("Deleting all roles with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesSchedulesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesSchedulesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesSchedulesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursesSchedulesByScheduleId(String scheduleId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.getById(SCHEDULES_TABLE, Schedule.class, scheduleId, okapiHeaders, vertxContext,
        GetCoursesSchedulesByScheduleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursesSchedulesByScheduleId(String scheduleId, String lang,
      Schedule entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(SCHEDULES_TABLE, entity, scheduleId, okapiHeaders, vertxContext,
        PutCoursesSchedulesByScheduleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesSchedulesByScheduleId(String scheduleId, String lang,
      Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    PgUtil.deleteById(SCHEDULES_TABLE, scheduleId, okapiHeaders, vertxContext,
        DeleteCoursesSchedulesByScheduleIdResponse.class, asyncResultHandler);
  }

}
