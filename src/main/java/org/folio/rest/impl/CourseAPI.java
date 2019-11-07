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
import java.util.logging.Level;
import javax.ws.rs.core.Response;
import org.folio.coursereserves.util.CRUtil;
import org.folio.cql2pgjson.CQL2PgJSON;
import org.folio.cql2pgjson.exception.FieldException;
import org.folio.rest.RestVerticle;
import org.folio.rest.jaxrs.model.Copyrightstatus;
import org.folio.rest.jaxrs.model.Copyrightstatuses;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.CourseListingObject;
import org.folio.rest.jaxrs.model.Courselisting;
import org.folio.rest.jaxrs.model.Courselistings;
import org.folio.rest.jaxrs.model.Courses;
import org.folio.rest.jaxrs.model.Coursetype;
import org.folio.rest.jaxrs.model.Coursetypes;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.Departments;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.Instructors;
import org.folio.rest.jaxrs.model.Processingstatus;
import org.folio.rest.jaxrs.model.Processingstatuses;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.Reserves;
import org.folio.rest.jaxrs.model.Role;
import org.folio.rest.jaxrs.model.Roles;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.Terms;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.Criteria.Limit;
import org.folio.rest.persist.Criteria.Offset;
import org.folio.rest.persist.PgUtil;
import static org.folio.rest.persist.PgUtil.postgresClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.persist.cql.CQLWrapper;
import org.folio.rest.tools.utils.TenantTool;
import org.folio.rest.tools.utils.ValidationHelper;

public class CourseAPI implements org.folio.rest.jaxrs.resource.Coursereserves {
  
  public static final Logger logger = LoggerFactory.getLogger(
          CourseAPI.class);
  
  public static final String COURSES_TABLE = "coursereserves_courses";
  public static final String COURSE_LISTINGS_TABLE = "coursereserves_courselistings";
  public static final String RESERVES_TABLE = "coursereserves_reserves";
  public static final String INSTRUCTORS_TABLE = "coursereserves_instructors";
  public static final String PROCESSING_STATUSES_TABLE = "coursereserves_processingstates";
  public static final String TERMS_TABLE = "coursereserves_terms";
  public static final String DEPARTMENTS_TABLE = "coursereserves_departments";
  public static final String COPYRIGHT_STATUSES_TABLE = "coursereserves_copyrightstates";
  public static final String COURSE_TYPES_TABLE = "coursereserves_coursetypes";
  public static final String ROLES_TABLE = "coursereserves_roles";
  public static final String BASE_PREFIX = "/coursereserves/";
  public static final String COURSES_PREFIX = BASE_PREFIX + "/courses";
  public static final String COURSE_LISTINGS_PREFIX = BASE_PREFIX + "/courselistings";
  public static final String RESERVES_PREFIX = BASE_PREFIX + "/reserves";
  public static final String INSTRUCTORS_PREFIX = BASE_PREFIX + "/instructors";
  public static final String TERMS_PREFIX = BASE_PREFIX + "/terms";
  public static final String COPYRIGHT_STATUSES_PREFIX = "/copyrightstates";
  public static final String PROCESSING_STATUSES_PREFIX = "/processingstates";
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
    if(!entity.getCourseListingId().equals(listingId)) {
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
    if(!entity.getCourseListingId().equals(listingId)) {
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
    if(!entity.getCourseListingId().equals(listingId)) {
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
  public void getCoursereservesRoles(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
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
  public void getCoursereservesCoursetypes(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(COURSE_TYPES_TABLE, Coursetype.class, Coursetypes.class, query,
        offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesCoursetypesResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursereservesCoursetypes(String lang, Coursetype entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(COURSE_TYPES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesCoursetypesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCoursetypes(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", COURSE_TYPES_TABLE);
        logger.info(String.format("Deleting all courses types with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCoursetypesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCoursetypesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCoursetypesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCoursetypesByTypeId(String typeId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(COURSE_TYPES_TABLE, Coursetype.class, typeId, okapiHeaders,
        vertxContext, GetCoursereservesCoursetypesByTypeIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesCoursetypesByTypeId(String typeId, String lang,
      Coursetype entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COURSE_TYPES_TABLE, entity, typeId, okapiHeaders, vertxContext,
        PutCoursereservesCoursetypesByTypeIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCoursetypesByTypeId(String typeId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COURSE_TYPES_TABLE, typeId, okapiHeaders, vertxContext,
        DeleteCoursereservesCoursetypesByTypeIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursereservesDepartments(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(DEPARTMENTS_TABLE, Department.class, Departments.class, query,
        offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesDepartmentsResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursereservesDepartments(String lang, Department entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(DEPARTMENTS_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesDepartmentsResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesDepartments(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", DEPARTMENTS_TABLE);
        logger.info(String.format("Deleting all courses listings with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesDepartmentsResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesDepartmentsResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesDepartmentsResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesDepartmentsByDepartmentId(String departmentId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(DEPARTMENTS_TABLE, Department.class, departmentId, okapiHeaders,
        vertxContext, GetCoursereservesDepartmentsByDepartmentIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesDepartmentsByDepartmentId(String departmentId,
      String lang, Department entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(DEPARTMENTS_TABLE, entity, departmentId, okapiHeaders,
        vertxContext, PutCoursereservesDepartmentsByDepartmentIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesDepartmentsByDepartmentId(String departmentId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(DEPARTMENTS_TABLE, departmentId, okapiHeaders, vertxContext,
        DeleteCoursereservesDepartmentsByDepartmentIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesProcessingstatuses(String query, int offset,
      int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(PROCESSING_STATUSES_TABLE, Processingstatus.class, Processingstatuses.class,
        query, offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesProcessingstatusesResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursereservesProcessingstatuses(String lang, 
      Processingstatus entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(PROCESSING_STATUSES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesProcessingstatusesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesProcessingstatuses(Map<String,
      String> okapiHeaders, Handler<AsyncResult<Response>> asyncResultHandler,
      Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", PROCESSING_STATUSES_TABLE);
        logger.info(String.format("Deleting all processing statuses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesProcessingstatusesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesProcessingstatusesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
            DeleteCoursereservesProcessingstatusesResponse.respond500WithTextPlain(
            getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesProcessingstatusesByStatusId(String statusId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(PROCESSING_STATUSES_TABLE, Processingstatus.class, statusId,
        okapiHeaders, vertxContext,
        GetCoursereservesProcessingstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesProcessingstatusesByStatusId(String statusId,
      String lang, Processingstatus entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(PROCESSING_STATUSES_TABLE, entity, statusId, okapiHeaders,
        vertxContext, PutCoursereservesProcessingstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesProcessingstatusesByStatusId(String statusId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(PROCESSING_STATUSES_TABLE, statusId, okapiHeaders,
        vertxContext, DeleteCoursereservesProcessingstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesCopyrightstatuses(String query, int offset, 
      int limit, String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(COPYRIGHT_STATUSES_TABLE, Copyrightstatus.class, Copyrightstatuses.class,
        query, offset, limit, okapiHeaders, vertxContext,
        GetCoursereservesCopyrightstatusesResponse.class, asyncResultHandler);
  }

  @Override
  public void postCoursereservesCopyrightstatuses(String lang, 
      Copyrightstatus entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(COPYRIGHT_STATUSES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesCopyrightstatusesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCopyrightstatuses(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", COPYRIGHT_STATUSES_TABLE);
        logger.info(String.format("Deleting all copyright statuses with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCopyrightstatusesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCopyrightstatusesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCopyrightstatusesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCopyrightstatusesByStatusId(String statusId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.getById(COPYRIGHT_STATUSES_TABLE, Copyrightstatus.class, statusId,
        okapiHeaders, vertxContext,
        GetCoursereservesCopyrightstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void putCoursereservesCopyrightstatusesByStatusId(String statusId,
      String lang, Copyrightstatus entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COPYRIGHT_STATUSES_TABLE, entity, statusId, okapiHeaders,
        vertxContext, PutCoursereservesCopyrightstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCopyrightstatusesByStatusId(String statusId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COPYRIGHT_STATUSES_TABLE, statusId, okapiHeaders,
        vertxContext, DeleteCoursereservesCopyrightstatusesByStatusIdResponse.class,
        asyncResultHandler);
  }

  @Override
  public void getCoursereservesCourses(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {


    try {
      CQLWrapper cqlWrapper = getCQL(query, limit, offset, COURSES_TABLE);
      PostgresClient postgresClient = postgresClient(vertxContext, okapiHeaders);
      postgresClient.get(COURSES_TABLE, Course.class, cqlWrapper, true, reply -> {
        if(reply.failed()) {
          String message = logAndSaveError(reply.cause());
          asyncResultHandler.handle(Future.succeededFuture(
              GetCoursereservesCoursesResponse.respond500WithTextPlain(
                  getErrorResponse(message))));
        } else {
          List<Course> courseList = reply.result().getResults();
          CRUtil.expandListOfCourses(courseList, okapiHeaders, vertxContext).setHandler(
            res -> {
              if(res.failed()) {
                String message = logAndSaveError(res.cause());
                asyncResultHandler.handle(Future.succeededFuture(
                    GetCoursereservesCoursesResponse.respond500WithTextPlain(
                        getErrorResponse(message))));
              } else {
                Courses courses = new Courses();
                courses.setCourses(res.result());
                courses.setTotalRecords(reply.result().getResultInfo().getTotalRecords());
                asyncResultHandler.handle(Future.succeededFuture(
                    GetCoursereservesCoursesResponse.respond200WithApplicationJson(courses)));
              }
            }
          );
        }
      });
    } catch (Exception e) {
      String message = logAndSaveError(e);
      if(isCQLError(e)) {
        message = String.format("CQL Error: %s", message);
      }
      asyncResultHandler.handle(Future.succeededFuture(
          GetCoursereservesCoursesResponse.respond500WithTextPlain(
          getErrorResponse(message))));
    }
  }

  @Override
  public void postCoursereservesCourses(String lang, Course entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(COURSES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesCoursesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCourses(Map<String, String> okapiHeaders,
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
                    DeleteCoursereservesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCoursesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCoursesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesCoursesByCourseId(String courseId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
      String tenantId = getTenant(okapiHeaders);
      PostgresClient pgClient = getPGClient(vertxContext, tenantId);
      Criteria idCrit = new Criteria()
          .addField(ID_FIELD)
          .setOperation("=")
          .setVal(courseId);
      pgClient.get(COURSES_TABLE, Course.class,
          new Criterion(idCrit), true, false, getReply -> {
        if (getReply.failed()) {
          String message = logAndSaveError(getReply.cause());
          asyncResultHandler.handle(Future.succeededFuture(
              GetCoursereservesCoursesByCourseIdResponse.respond500WithTextPlain(
              getErrorResponse(message))));
        } else {
          List<Course> courseList = getReply.result().getResults();
          if (courseList.isEmpty()) {
            asyncResultHandler.handle(Future.succeededFuture(
                GetCoursereservesCoursesByCourseIdResponse
                .respond404WithTextPlain(String.format(
                "No Course exists with id '%s'", courseId))));
          } else {
            Course course = courseList.get(0);
            CRUtil.getExpandedCourse(course, okapiHeaders, vertxContext)
                .setHandler(expandCourseRes -> {
              if(expandCourseRes.failed()) {
                  String message = logAndSaveError(expandCourseRes.cause());
                    asyncResultHandler.handle(Future.succeededFuture(
                    GetCoursereservesCoursesByCourseIdResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
              } else {
                asyncResultHandler.handle(Future.succeededFuture(
                    GetCoursereservesCoursesByCourseIdResponse
                    .respond200WithApplicationJson(expandCourseRes.result())));
              }
            });
          }
        }
      });
    } catch(Exception e) {
      String message = logAndSaveError(e);
      asyncResultHandler.handle(Future.succeededFuture(
          GetCoursereservesCoursesByCourseIdResponse.respond500WithTextPlain(
          getErrorResponse(message))));
    }
  }

  @Override
  public void putCoursereservesCoursesByCourseId(String courseId, String lang,
      Course entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(COURSES_TABLE, entity, courseId, okapiHeaders, vertxContext,
        PutCoursereservesCoursesByCourseIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesCoursesByCourseId(String courseId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(COURSES_TABLE, courseId, okapiHeaders, vertxContext,
        DeleteCoursereservesCoursesByCourseIdResponse.class, asyncResultHandler);
  }

  @Override
  public void getCoursereservesReserves(String query, int offset, int limit,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.get(RESERVES_TABLE, Reserve.class, Reserves.class, query, offset,
        limit, okapiHeaders, vertxContext, GetCoursereservesReservesResponse.class,
        asyncResultHandler);
  }

  @Override
  public void postCoursereservesReserves(String lang, Reserve entity,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.post(RESERVES_TABLE, entity, okapiHeaders, vertxContext,
        PostCoursereservesReservesResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesReserves(Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    try {
        String tenantId = getTenant(okapiHeaders);
        PostgresClient pgClient = getPGClient(vertxContext, tenantId);
        final String DELETE_ALL_QUERY = String.format("DELETE FROM %s_%s.%s",
                tenantId, "mod_courses", RESERVES_TABLE);
        logger.info(String.format("Deleting all reserves with query %s",
                DELETE_ALL_QUERY));
        pgClient.execute(DELETE_ALL_QUERY, mutateReply -> {
          if(mutateReply.failed()) {
            String message = logAndSaveError(mutateReply.cause());
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCoursesResponse.respond500WithTextPlain(
                    getErrorResponse(message))));
          } else {
            asyncResultHandler.handle(Future.succeededFuture(
                    DeleteCoursereservesCoursesResponse.noContent().build()));
          }
        });
      } catch(Exception e) {
        String message = logAndSaveError(e);
        asyncResultHandler.handle(Future.succeededFuture(
                DeleteCoursereservesCoursesResponse.respond500WithTextPlain(
                getErrorResponse(message))));
      }
  }

  @Override
  public void getCoursereservesReservesByReserveId(String reserveId, String lang,
      Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
   PgUtil.getById(RESERVES_TABLE, Reserve.class, reserveId, okapiHeaders,
       vertxContext, GetCoursereservesReservesByReserveIdResponse.class,
       asyncResultHandler);
  }

  @Override
  public void putCoursereservesReservesByReserveId(String reserveId, String lang,
      Reserve entity, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.put(RESERVES_TABLE, entity, reserveId, okapiHeaders, vertxContext,
        PutCoursereservesReservesByReserveIdResponse.class, asyncResultHandler);
  }

  @Override
  public void deleteCoursereservesReservesByReserveId(String reserveId,
      String lang, Map<String, String> okapiHeaders,
      Handler<AsyncResult<Response>> asyncResultHandler, Context vertxContext) {
    PgUtil.deleteById(RESERVES_TABLE, reserveId, okapiHeaders, vertxContext,
        DeleteCoursereservesReservesByReserveIdResponse.class, asyncResultHandler);
  }

}