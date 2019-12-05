package org.folio.coursereserves.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import static org.folio.rest.impl.CourseAPI.COURSE_LISTINGS_TABLE;
import static org.folio.rest.impl.CourseAPI.COURSE_TYPES_TABLE;
import static org.folio.rest.impl.CourseAPI.DEPARTMENTS_TABLE;
import static org.folio.rest.impl.CourseAPI.INSTRUCTORS_TABLE;
import static org.folio.rest.impl.CourseAPI.TERMS_TABLE;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.CopiedItem;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.Courselisting;
import org.folio.rest.jaxrs.model.CourseListingObject;
import org.folio.rest.jaxrs.model.CourseTypeObject;
import org.folio.rest.jaxrs.model.Coursetype;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.DepartmentObject;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.InstructorObject;
import org.folio.rest.jaxrs.model.PatronGroupObject;
import org.folio.rest.jaxrs.model.Publication;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.TermObject;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import static org.folio.rest.persist.PgUtil.postgresClient;
import org.folio.rest.persist.PostgresClient;


public class CRUtil {

  public static final Logger logger = LoggerFactory.getLogger(
          CRUtil.class);


  public static Future<Void> populateReserveInventoryCache(Reserve reserve,
      Map<String, String> okapiHeaders, Context context) {
    Future<Void> future = Future.future();
    String itemId = reserve.getItemId();
    lookupItemHoldingsInstanceByItemId(itemId, okapiHeaders, context)
        .setHandler(inventoryRes -> {
      if(inventoryRes.failed()) {
        future.fail(inventoryRes.cause());
      } else {
        JsonObject result = inventoryRes.result();
        JsonObject itemJson = result.getJsonObject("item");
        JsonObject holdingsJson = result.getJsonObject("holdings");
        JsonObject instanceJson = result.getJsonObject("instance");
        CopiedItem copiedItem = new CopiedItem();
        copiedItem.setBarcode(itemJson.getString("barcode"));
        copiedItem.setVolume(itemJson.getString("volume"));
        copiedItem.setEnumeration(itemJson.getString("enumeration"));
        try {
          copiedItem.setCopy(itemJson.getJsonArray("copyNumbers").getString(0));
        } catch(Exception e) {
          logger.info("Unable to copy copyNumbers field from item: " + e.getLocalizedMessage());
        }
        try {
          JsonObject eAJson = itemJson.getJsonArray("electronicAccess").getJsonObject(0);
          copiedItem.setUri(eAJson.getString("uri"));
          copiedItem.setUrl(eAJson.getString("publicNote"));
        } catch(Exception e) {
          logger.info("Unable to copy electronic access field from item: " + e.getLocalizedMessage());
        }
        copiedItem.setPermanentLocationId(holdingsJson.getString("permanentLocationId"));
        copiedItem.setTemporaryLocationId(instanceJson.getString("temporaryLocationId"));
        copiedItem.setCallNumber(holdingsJson.getString("callNumber"));
        JsonArray contributors = instanceJson.getJsonArray("contributors");
        if(contributors != null && contributors.size() > 0) {
          List<Contributor> contributorList = new ArrayList<>();
          for(int i = 0; i < contributors.size(); i++) {
            JsonObject contributorJson = contributors.getJsonObject(i);
            Contributor contributor = new Contributor();
            contributor.setContributorNameTypeId(contributorJson.getString("contributorNameTypeId"));
            contributor.setContributorTypeId(contributorJson.getString("contributorTypeId"));
            contributor.setContributorTypeText(contributorJson.getString("contributorTypeText"));
            contributor.setName(contributorJson.getString("name"));
            contributor.setPrimary(contributorJson.getBoolean("primary"));
            contributorList.add(contributor);
          }
          copiedItem.setContributors(contributorList);
        }

        JsonArray publishers = instanceJson.getJsonArray("publication");
        if(publishers != null && publishers.size() > 0) {
          List<Publication> publisherList = new ArrayList<>();
          for(int i = 0; i < publishers.size(); i++) {
            JsonObject publisherJson = publishers.getJsonObject(i);
            Publication publication = new Publication();
            publication.setPlace(publisherJson.getString("place"));
            publication.setPublisher(publisherJson.getString("publisher"));
            publication.setDateOfPublication(publisherJson.getString("dateOfPublication"));
            publication.setRole(publisherJson.getString("role"));
            publisherList.add(publication);
          }
          copiedItem.setPublication(publisherList);
        }

        reserve.setCopiedItem(copiedItem);
      }
    });
    return future;
  }

  public static Future<JsonObject> lookupItemHoldingsInstanceByItemId(String itemId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    JsonObject result = new JsonObject();
    String itemPath = "/item-storage/items/";
    String holdingsPath = "/holdings-storage/holdings/";
    String instancePath = "/instance-storage/instances/";
    makeOkapiRequest(context.owner(), okapiHeaders, itemPath + itemId, HttpMethod.GET,
        null, null, 200).setHandler(itemRes -> {
      if(itemRes.failed()) {
        future.fail(itemRes.cause());
      } else {
        JsonObject itemJson = itemRes.result();
        String holdingsId = itemJson.getString("holdingsRecordId");
        result.put("item", itemJson);
        makeOkapiRequest(context.owner(), okapiHeaders, holdingsPath + holdingsId,
            HttpMethod.GET, null, null, 200).setHandler(holdingsRes -> {
          if(holdingsRes.failed()) {
            future.fail(holdingsRes.cause());
          } else {
            JsonObject holdingsJson = holdingsRes.result();
            String instanceId = holdingsJson.getString("instanceId");
            result.put("holdings", holdingsJson);
            makeOkapiRequest(context.owner(), okapiHeaders, instancePath + instanceId,
                HttpMethod.GET, null, null, 200).setHandler(instanceRes -> {
              if(instanceRes.failed()) {
                future.fail(instanceRes.cause());
              } else {
                JsonObject instanceJson = instanceRes.result();
                result.put("instance", instanceJson);
                future.complete(result);
              }
            });
          }
        });
      }
    });
    return future;
  }
  public static Future<JsonObject> lookupUserAndGroupByUserId(String userId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    String userPath = "/users/" + userId;
    JsonObject result = new JsonObject();
    makeOkapiRequest(context.owner(), okapiHeaders, userPath, HttpMethod.GET,
        null, null, 200).setHandler(userRes -> {
      try {
        if(userRes.failed()) {
          future.fail(userRes.cause());
        } else {
          result.put("user", userRes.result());
          String groupId = userRes.result().getString("patronGroup");
          String groupPath = "/groups/" + groupId;
          makeOkapiRequest(context.owner(), okapiHeaders, groupPath, HttpMethod.GET,
              null, null, 200).setHandler(groupRes -> {
            try {
              if(groupRes.failed()) {
                future.fail(groupRes.cause());
              } else {
                result.put("group", groupRes.result());
                //TODO, delete comments
                /*
                PatronGroupObject patronGroupObject = new PatronGroupObject();
                patronGroupObject.setId(groupRes.result().getString("id"));
                patronGroupObject.setGroup(groupRes.result().getString("group"));
                patronGroupObject.setDesc(groupRes.result().getString("desc"));
                */
                future.complete(result);
              }
            } catch(Exception e) {
              future.fail(e);
            }
          });
        }
      } catch(Exception e) {
        future.fail(e);
      }
    });
    return future;
  }

  public static Future<JsonObject> makeOkapiRequest(Vertx vertx,
      Map<String, String> okapiHeaders, String requestPath, HttpMethod method,
      Map<String, String> extraHeaders, String payload, Integer expectedCode) {
    Future<JsonObject> future = Future.future();
    HttpClient client = vertx.createHttpClient();
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();
    CaseInsensitiveHeaders originalHeaders = new CaseInsensitiveHeaders();
    originalHeaders.setAll(okapiHeaders);
    String okapiUrl = originalHeaders.get("x-okapi-url");
    String requestUrl = okapiUrl + requestPath;
    headers.add("x-okapi-token", originalHeaders.get("x-okapi-token"));
    headers.add("x-okapi-tenant", originalHeaders.get("x-okapi-tenant"));
    headers.add("content-type", "application/json");
    headers.add("accept", "application/json");
    if(extraHeaders != null) {
      for(Map.Entry<String, String> entry : extraHeaders.entrySet()) {
        headers.add(entry.getKey(), entry.getValue());
      }
    }
    HttpClientRequest request = client.requestAbs(method, requestUrl);
    for(Map.Entry entry : headers.entries()) {
      String key = (String)entry.getKey();
      String value = (String)entry.getValue();
      if( key != null && value != null) {
        request.putHeader(key, value);
      }
    }
    request.exceptionHandler(e -> { future.fail(e); });
    request.handler( requestRes -> {
      requestRes.bodyHandler(bodyHandlerRes -> {
        try {
          String response = bodyHandlerRes.toString();
          if(expectedCode != requestRes.statusCode()) {
            future.fail(String.format("Expected status code %s for %s request to url %s, got %s: %s",
                expectedCode, method.toString(), requestUrl, requestRes.statusCode(),
                response));
          } else {
            JsonObject responseJson = new JsonObject(response);
            future.complete(responseJson);
          }
        } catch(Exception e) {
          future.fail(e);
        }
      });
    });
    if(method == HttpMethod.PUT || method == HttpMethod.POST) {
      request.end(payload);
    } else {
      request.end();
    }
    return future;
  }

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
                  lookupInstructorsForCourseListing(courseListingId, okapiHeaders.get("X-OKAPI-TENANT"),
                      context).setHandler(instructorLookupReply -> {
                    if(instructorLookupReply.failed()) {
                      future.fail(instructorLookupReply.cause());
                    } else {
                      List<Instructor> instructorList = instructorLookupReply.result();
                      List<InstructorObject> instructorObjectList = new ArrayList<>();
                      for(Instructor instructor : instructorList) {
                        InstructorObject instructorObject = new InstructorObject();
                        instructorObject.setBarcode(instructor.getBarcode());
                        instructorObject.setCourseListingId(instructor.getCourseListingId());
                        instructorObject.setId(instructor.getId());
                        instructorObject.setName(instructor.getName());
                        instructorObject.setPatronGroup(instructor.getPatronGroup());
                        instructorObject.setPatronGroupObject(instructor.getPatronGroupObject());
                        instructorObjectList.add(instructorObject);                        
                      }
                      result.setInstructorObjects(instructorObjectList);
                      future.complete(result);
                    }
                  });
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

  public static Future<List<Instructor>> lookupInstructorsForCourseListing(
      String courseListingId, String tenantId, Context context) {
    Future<List<Instructor>> future = Future.future();
    PostgresClient postgresClient = PostgresClient.getInstance(context.owner(), tenantId);
    Criteria idCrit = new Criteria();
    idCrit.addField("'courseListingId'");
    idCrit.setOperation("=");
    idCrit.setVal(courseListingId);
    Criterion criterion = new Criterion(idCrit);
    logger.info("Requesting instructor records with criterion: " + criterion.toString());
    postgresClient.get(INSTRUCTORS_TABLE, Instructor.class, criterion,
        true, false, res -> {
      if(res.failed()) {
        future.fail(res.cause());
      } else {
        List<Instructor> instructorList = new ArrayList<>();      
        for(Instructor instructor : res.result().getResults()) {
          instructorList.add(instructor);          
        }
        future.complete(instructorList);
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
          expandedCourseListing.setCourseTypeObject(courseListing.getCourseTypeObject());
          expandedCourseListing.setExternalId(courseListing.getExternalId());
          expandedCourseListing.setId(courseListing.getId());
          expandedCourseListing.setLocationId(courseListing.getLocationId());
          expandedCourseListing.setRegistrarId(courseListing.getRegistrarId());
          expandedCourseListing.setServicepointId(courseListing.getServicepointId());
          expandedCourseListing.setTermId(courseListing.getTermId());
          expandedCourseListing.setTermObject(courseListing.getTermObject());
          expandedCourseListing.setInstructorObjects(courseListing.getInstructorObjects());
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
