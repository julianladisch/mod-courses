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
import org.folio.rest.jaxrs.model.Copyrightstatus;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.Courselisting;
import org.folio.rest.jaxrs.model.Courselistings;
import org.folio.rest.jaxrs.model.Courses;
import org.folio.rest.jaxrs.model.Coursetype;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.Instructors;
import org.folio.rest.jaxrs.model.Processingstatus;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.Reserves;
import org.folio.rest.jaxrs.model.Role;
import org.folio.rest.jaxrs.model.Roles;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.Terms;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.tools.utils.ValidationHelper;

public class CourseAPI implements org.folio.rest.jaxrs.resource.Coursereserves {
  
  public static final Logger logger = LoggerFactory.getLogger(
          CourseAPI.class);
  
  public static final String COURSES_TABLE = "courses_courses";
  public static final String COURSE_LISTINGS_TABLE = "courses_courselistings";
  public static final String RESERVES_TABLE = "courses_reserves";
  public static final String INSTRUCTORS_TABLE = "courses_instructors";
  public static final String PROCESSING_STATES_TABLE = "courses_processingstates";
  public static final String TERMS_TABLE = "courses_terms";
  public static final String DEPARTMENTS_TABLE = "courses_departments";
  public static final String COPYRIGHT_STATES_TABLE = "courses_copyrightstates";
  public static final String ROLES_TABLE = "courses_roles";
  public static final String BASE_PREFIX = "/courses/";
  public static final String COURSES_PREFIX = BASE_PREFIX + "/courses";
  public static final String COURSE_LISTINGS_PREFIX = BASE_PREFIX + "/courselistings";
  public static final String RESERVES_PREFIX = BASE_PREFIX + "/reserves";
  public static final String INSTRUCTORS_PREFIX = BASE_PREFIX + "/instructors";
  public static final String TERMS_PREFIX = BASE_PREFIX + "/terms";
  public static final String COPYRIGHT_STATES_PREFIX = "/copyrightstates";
  public static final String PROCESSING_STATES_PREFIX = "/processingstates";
  public static final String DEPARTMENTS_PREFIX = "/departments";
  public static final String ROLES_PREFIX = "/roles";
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
/*
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



  @Override
  public void getCoursesCourselistings(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(COURSE_LISTINGS_TABLE, Courselisting.class, Courselistings.class,
        query, offset, limit, okapiHeaders, vertxContext,
        GetCoursesCourselistingsResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursesCourselistings(String lang, Courselisting entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(COURSE_LISTINGS_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursesCourselistingsResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursesCourselistings(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", COURSE_LISTINGS_TABLE);
        logger.info(String.format("Deleting all course listings with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCourselistingsResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursesCourselistingsResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursesCourselistingsResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  */

  @Override
  public void getCoursereservesCourselistings(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(COURSE_LISTINGS_TABLE, Courselisting.class, Courselistings.class,
        query, offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesCourselistingsResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursereservesCourselistings(String lang, Courselisting entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(COURSE_LISTINGS_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesCourselistingsResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourselistings(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", COURSE_LISTINGS_TABLE);
        logger.info(String.format("Deleting all courses listings with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCourselistingsResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCourselistingsByListingId(String listingId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(COURSE_LISTINGS_TABLE, Courselisting.class, listingId,
        okapiHeaders, vertxContext,
        GetCoursereservesCourselistingsByListingIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursereservesCourselistingsByListingId(String listingId,
      String lang, Courselisting entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COURSE_LISTINGS_TABLE, entity, listingId, okapiHeaders, vertxContext,
        PutCoursereservesCourselistingsByListingIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourselistingsByListingId(String listingId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COURSE_LISTINGS_TABLE, listingId, okapiHeaders, vertxContext,
        DeleteCoursereservesCourselistingsByListingIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesCourselistingsCoursesByListingId(String listingId,
      String query, int offset, int limit, String lang, Map<String,
          String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
          Context vertxContext) {
    String courseQueryClause = String.format("courseListingId = %s", listingId);
    if(query == null || query.isEmpty()) {
      query = courseQueryClause;
    } else {
      query = String.format("(%s) AND %s", courseQueryClause, query);
    }
    PgUtil.get(COURSES_TABLE, Course.class, Courses.class, query, offset, limit,
        okapiHeaders, vertxContext,
        GetCoursereservesCourselistingsCoursesByListingIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesCourselistingsCoursesByListingId(String listingId,
      String lang, Course entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getCourseListingId() != listingId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursereservesCourselistingsCoursesByListingIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("listingId", entity.getCourseListingId(),
                      String.format("listingId should be %s", listingId)))));
    } else {
      PgUtil.post(COURSES_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursereservesCourselistingsCoursesByListingIdResponse.class,
          asyncResultHandler);
    }
  }

  @Override
  public void deleteCoursereservesCourselistingsCoursesByListingId(String listingId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format(
            "DELETE FROM %s_%s.%s WHERE jsonb->>'courseListingId' = '%s'",
                tenantId, "mod_courses", COURSES_TABLE, listingId);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsCoursesByListingIdResponse
                        .respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsCoursesByListingIdResponse
                        .noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCourselistingsCoursesByListingIdResponse
                    .respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCourselistingsCoursesByListingIdAndCourseId(
      String listingId, String courseId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(COURSES_TABLE, Course.class, courseId, okapiHeaders,
        vertxContext,
        GetCoursereservesCourselistingsCoursesByListingIdAndCourseIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesCourselistingsCoursesByListingIdAndCourseId(
      String listingId, String courseId, String lang,
      Course entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COURSES_TABLE, entity, courseId, okapiHeaders, vertxContext,
        PutCoursereservesCourselistingsCoursesByListingIdAndCourseIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourselistingsCoursesByListingIdAndCourseId(
      String listingId, String courseId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COURSES_TABLE, courseId, okapiHeaders, vertxContext,
        DeleteCoursereservesCourselistingsCoursesByListingIdAndCourseIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesCourselistingsInstructorsByListingId(String listingId,
      String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String instructorQueryClause = String.format("courseListingId = %s", listingId);
    if(query == null || query.isEmpty()) {
      query = instructorQueryClause;
    } else {
      query = String.format("(%s) AND %s", instructorQueryClause, query);
    }
    PgUtil.get(INSTRUCTORS_TABLE, Instructor.class, Instructors.class, query,
        offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesCourselistingsCoursesByListingIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesCourselistingsInstructorsByListingId(
      String listingId, String lang, Instructor entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getCourseListingId() != listingId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursereservesCourselistingsInstructorsByListingIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("listingId", entity.getCourseListingId(),
                      String.format("listingId should be %s", listingId)))));
    } else {
      PgUtil.post(INSTRUCTORS_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursereservesCourselistingsInstructorsByListingIdResponse.class,
          asyncResultHandler);
    }
  }

  @Override
  public void deleteCoursereservesCourselistingsInstructorsByListingId(
      String listingId, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      String tenantId = getTenant(okapiHeaders);
      PostgresClient pgClient = getPGClient(vertxContext, tenantId);
      final String DELETE_ALL_QUERY = String.format(
          "DELETE FROM %s_%s.%s WHERE jsonb->>'courseListingId' = '%s'",
              tenantId, "mod_courses", INSTRUCTORS_TABLE, listingId);
      logger.info(String.format("Deleting all instructors with query %s",
              DELETE_ALL_QUERY));
      pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
        if(mutateReply.failed()) {
          String message = logAndSaveError(mutateReply.cause());
          asyncResultHandler.handle(Future.succeededFuture(
                  DeleteCoursereservesCourselistingsInstructorsByListingIdResponse
                      .respond500WithTextPlain(
                  getErrorResponse(message))));
        } else {
          asyncResultHandler.handle(Future.succeededFuture(
                  DeleteCoursereservesCourselistingsInstructorsByListingIdResponse
                      .noContent().build()));
        }
      });
    } catch(Exception e) {
      String message = logAndSaveError(e);
      asyncResultHandler.handle(Future.succeededFuture(
              DeleteCoursereservesCourselistingsInstructorsByListingIdResponse
                  .respond500WithTextPlain(
              getErrorResponse(message))));
    }
  }

  @Override
  public void getCoursereservesCourselistingsInstructorsByListingIdAndInstructorId(
      String listingId, String instructorId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(INSTRUCTORS_TABLE, Instructor.class, instructorId, okapiHeaders,
        vertxContext,
        GetCoursereservesCourselistingsInstructorsByListingIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesCourselistingsInstructorsByListingIdAndInstructorId(
      String listingId, String instructorId, String lang, Instructor entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(INSTRUCTORS_TABLE, entity, instructorId, okapiHeaders, vertxContext,
        PutCoursereservesCourselistingsInstructorsByListingIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourselistingsInstructorsByListingIdAndInstructorId(
      String listingId, String instructorId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(INSTRUCTORS_TABLE, instructorId, okapiHeaders, vertxContext,
        DeleteCoursereservesCourselistingsInstructorsByListingIdAndInstructorIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesCourselistingsReservesByListingId(String listingId,
      String query, int offset, int limit, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    String courseListingQueryClause = String.format("courseListingId = %s", listingId);
    if(query == null || query.isEmpty()) {
      query = courseListingQueryClause;
    } else {
      query = String.format("(%s) AND %s", courseListingQueryClause, query);
    }
    PgUtil.get(RESERVES_TABLE, Reserve.class, Reserves.class, query, offset,
        limit, okapiHeaders, vertxContext,
        GetCoursereservesCourselistingsReservesByListingIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesCourselistingsReservesByListingId(String listingId,
      String lang, Reserve entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    if(entity.getCourseListingId() != listingId) {
      asyncResultHandler.handle(Future.succeededFuture(
          PostCoursereservesCourselistingsReservesByListingIdResponse
              .respond422WithApplicationJson(ValidationHelper
                  .createValidationErrorMessage("listingId", entity.getCourseListingId(),
                      String.format("listingId should be %s", listingId)))));
    } else {
      PgUtil.post(RESERVES_TABLE, entity, okapiHeaders, vertxContext,
          PostCoursereservesCourselistingsReservesByListingIdResponse.class,
          asyncResultHandler);
    }
  }

  @Override
  public void deleteCoursereservesCourselistingsReservesByListingId(String listingId,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format(
            "DELETE FROM %s_%s.%s WHERE jsonb->>'courseListingId' = '%s'",
                tenantId, "mod_courses", RESERVES_TABLE, listingId);
        logger.info(String.format("Deleting all courses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsReservesByListingIdResponse
                        .respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCourselistingsReservesByListingIdResponse
                        .noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCourselistingsReservesByListingIdResponse
                    .respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCourselistingsReservesByListingIdAndReserveId(
      String listingId, String reserveId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(RESERVES_TABLE, Reserve.class, reserveId, okapiHeaders,
        vertxContext,
        GetCoursereservesCourselistingsReservesByListingIdAndReserveIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesCourselistingsReservesByListingIdAndReserveId(
      String listingId, String reserveId, String lang, Reserve entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(RESERVES_TABLE, entity, reserveId, okapiHeaders, vertxContext,
        PutCoursereservesCourselistingsReservesByListingIdAndReserveIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourselistingsReservesByListingIdAndReserveId(
      String listingId, String reserveId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(RESERVES_TABLE, reserveId, okapiHeaders, vertxContext,
        DeleteCoursereservesCourselistingsReservesByListingIdAndReserveIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesRoles(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(ROLES_TABLE, Role.class, Roles.class, query, offset, limit,
        okapiHeaders, vertxContext, GetCoursereservesRolesResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesRoles(String lang, Role entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(ROLES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesRolesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesRoles(Map<String, String> okapiHeaders,
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
                    DeleteCoursereservesRolesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesRolesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesRolesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesRolesByRoleId(String roleId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(ROLES_TABLE, Role.class, roleId, okapiHeaders, vertxContext,
        GetCoursereservesRolesByRoleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursereservesRolesByRoleId(String roleId, String lang, 
      Role entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(ROLES_TABLE, entity, roleId, okapiHeaders, vertxContext,
        PutCoursereservesRolesByRoleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesRolesByRoleId(String roleId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(ROLES_TABLE, roleId, okapiHeaders, vertxContext,
        DeleteCoursereservesRolesByRoleIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursereservesTerms(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(TERMS_TABLE, Term.class, Terms.class, query, offset, limit,
        okapiHeaders, vertxContext, GetCoursereservesTermsResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesTerms(String lang, Term entity, 
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(TERMS_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesTermsResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesTerms(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
     String tenantId = getTenant(okapiHeaders);
     PostgresClient pgClient = getPGClient(vertxContext, tenantId);
     final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
             tenantId, "mod_courses", TERMS_TABLE);
     logger.info(String.format("Deleting all terms with query %s",
             DELETE_ALL_QUERY));
     pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
       if(mutateReply.failed()) {
         String message = logAndSaveError(mutateReply.cause());
         asyncResultHandler.handle(Future.succeededFuture(
                 DeleteCoursereservesTermsResponse.respond500WithTextPlain(
                 getErrorResponse(message))));
       } else {
         asyncResultHandler.handle(Future.succeededFuture(
                 DeleteCoursereservesTermsResponse.noContent().build()));
       }
     });
    } catch(Exception e) {
     String message = logAndSaveError(e);
     asyncResultHandler.handle(Future.succeededFuture(
             DeleteCoursereservesTermsResponse.respond500WithTextPlain(
             getErrorResponse(message))));
    }
  }

  @Override
  public void getCoursereservesTermsByTermId(String termId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(TERMS_TABLE, Term.class, termId, okapiHeaders, vertxContext,
        GetCoursereservesTermsByTermIdResponse.class, asyncResultHandler);
  }

  @Override
  public void putCoursereservesTermsByTermId(String termId, String lang,
      Term entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(TERMS_TABLE, entity, termId, okapiHeaders, vertxContext,
        PutCoursereservesTermsByTermIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesTermsByTermId(String termId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(TERMS_TABLE, termId, okapiHeaders, vertxContext,
        DeleteCoursereservesTermsByTermIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursereservesCoursetypes(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursereservesCoursetypes(String lang, Coursetype entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesCoursetypes(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesCoursetypesByTypeId(String typeId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursereservesCoursetypesByTypeId(String typeId, String lang, Coursetype entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesCoursetypesByTypeId(String typeId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesDepartments(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursereservesDepartments(String lang, Department entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesDepartments(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesDepartmentsByDepartmentId(String departmentId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursereservesDepartmentsByDepartmentId(String departmentId, String lang, Department entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesDepartmentsByDepartmentId(String departmentId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesProcessingstatuses(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursereservesProcessingstatuses(String lang, Processingstatus entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesProcessingstatuses(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesProcessingstatusesByStatusId(String statusId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursereservesProcessingstatusesByStatusId(String statusId, String lang, Processingstatus entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesProcessingstatusesByStatusId(String statusId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesCopyrightstatuses(String query, int offset, int limit, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void postCoursereservesCopyrightstatuses(String lang, Copyrightstatus entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesCopyrightstatuses(Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void getCoursereservesCopyrightstatusesByStatusId(String statusId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void putCoursereservesCopyrightstatusesByStatusId(String statusId, String lang, Copyrightstatus entity, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }

  @Override
  public void deleteCoursereservesCopyrightstatusesByStatusId(String statusId, String lang, Map<String, String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
  }


}
