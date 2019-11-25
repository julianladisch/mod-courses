package CourseAPITest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import static io.vertx.core.http.HttpMethod.PUT;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.ws.rs.HttpMethod;
import org.folio.coursereserves.util.CRUtil;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.jaxrs.model.PatronGroupObject;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CourseAPITest {
  static int port;
  static int okapiPort;
  private static Vertx vertx;
  private final Logger logger = LoggerFactory.getLogger(CourseAPITest.class);
  public static String baseUrl;
  public static String okapiUrl;
  public final static String COURSE_LISTING_1_ID = UUID.randomUUID().toString();
  public final static String TERM_1_ID = UUID.randomUUID().toString();
  public final static String TERM_2_ID = UUID.randomUUID().toString();
  public final static String COURSE_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_2_ID = UUID.randomUUID().toString();
  public final static String DEPARTMENT_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_TYPE_1_ID = UUID.randomUUID().toString();
  public final static String INSTRUCTOR_1_ID = UUID.randomUUID().toString();
  public final static String INSTRUCTOR_2_ID = UUID.randomUUID().toString();
  public static Map<String, String> okapiHeaders = new HashMap<>();
  public static CaseInsensitiveHeaders standardHeaders = new CaseInsensitiveHeaders();




  @Rule
  public Timeout rule = Timeout.seconds(200);

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    okapiPort = NetworkUtils.nextFreePort();
    baseUrl = "http://localhost:"+port+"/coursereserves";
    okapiUrl = "http://localhost:"+okapiPort;
    TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
    okapiHeaders.put("x-okapi-tenant", "diku");
    okapiHeaders.put("x-okapi-url", okapiUrl);
    standardHeaders.add("x-okapi-url", okapiUrl);
    vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port));
    DeploymentOptions okapiOptions = new DeploymentOptions()
        .setConfig(new JsonObject().put("port", okapiPort));
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }
    vertx.deployVerticle(RestVerticle.class.getName(), options, deployCourseRes -> {
      if(deployCourseRes.failed()) {
        context.fail(deployCourseRes.cause());
      } else {
        try {
          tenantClient.postTenant(null, postTenantRes -> {
            vertx.deployVerticle(OkapiMock.class.getName(), okapiOptions,
                deployOkapiRes -> {
              if(deployOkapiRes.failed()) {
                context.fail(deployOkapiRes.cause());
              } else {
                async.complete();
              }
            });
          });
        } catch(Exception e) {
          e.printStackTrace();
          context.fail(e);
        }
      }
    });
  }

  @AfterClass
  public static void afterClass(TestContext context) {
    Async async = context.async();
    vertx.close(context.asyncAssertSuccess( res -> {
      PostgresClient.stopEmbeddedPostgres();
      async.complete();
    }));
  }

  @Before
  public void beforeEach(TestContext context) {
    Async async = context.async();
    loadTerm1()
        .compose(f -> {
        return loadTerm2();
      })
      .compose(f -> {
        return loadCourseListing1();
      })
      .compose(f -> {
        return loadDepartment1();
      })
       .compose(f -> {
        return loadCourseListing1Instructor1();
      })
        .compose(f -> {
        return loadCourseListing1Instructor2();
      })
      .compose(f -> {
        return loadCourseType1();
      })
      .compose(f -> {
        return loadCourse1();
      })
      .compose(f -> {
        return loadCourse2();
      })
      .setHandler(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
  }

  @After
  public void afterEach(TestContext context) {
    Async async = context.async();
    deleteCourses()
        .compose(f -> {
          return deleteCourseListing1Instructors();
        })
        .compose(f -> {
          return deleteCourseListings();
        })
        .compose(f -> {
          return deleteTerms();
        }).compose(f -> {
          return deleteDepartments();
        }).compose(f -> {
          return deleteCourseTypes();
        }).setHandler(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
  }

  @Test
  public void dummyTest(TestContext context) {
    Async async = context.async();
    async.complete();
  }

  @Test
  public void getRoles(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/roles", GET, null, null, 200,
        "Get role listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getCourseTypes(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", GET, null, null, 200,
        "Get course types listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getProcessingStatuses(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/processingstatuses", GET, null, null, 200,
        "Get processing status listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getCourses(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses", GET, null, null, 200,
        "Get course listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson().getJsonArray("courses").getJsonObject(0);
          if(course.getJsonObject("courseListingObject") == null) {
            context.fail("No course listing object found");
            return;
          }
          if(course.getJsonObject("courseListingObject").getJsonObject("termObject") == null) {
            context.fail("No term object found in " + course.encode());
            return;
          }
          if(course.getJsonObject("departmentObject") == null) {
            context.fail("No department found in " + course.encode());
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getAllCoursesByQuery(TestContext context) {
     Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl +
        "/courses?query=cql.allRecords=1%20sortby%20name&limit=500", GET, null,
        null, 200, "Get courses by query").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson().getJsonArray("courses").getJsonObject(0);
          if(course.getJsonObject("courseListingObject") == null) {
            context.fail("No course listing object found");
            return;
          }
          if(res.result().getJson().getInteger("totalRecords") < 2) {
            context.fail("Expected at least two results");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

 @Test
  public void getReserves(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/reserves", GET, null, null, 200,
        "Get reserve listing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void getInstructorsForCourseListing1(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/"
        + COURSE_LISTING_1_ID + "/instructors", GET, null, null, 200,
        "Get instructors for courselisting 1").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          if(res.result().getJson().getInteger("totalRecords") < 2) {
            context.fail("Expected at least two instructors");
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getCourseById(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses/" + COURSE_1_ID, GET, null, null, 200,
        "Get course by id").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson();
          JsonObject courseListing = course.getJsonObject("courseListingObject");
          if(courseListing == null) {
            context.fail("No course listing object found");
            return;
          }
          if(!courseListing.getString("id").equals(COURSE_LISTING_1_ID)) {
            context.fail("Bad id for course listing object, got " +
                courseListing.getString("id") + " expected " + COURSE_LISTING_1_ID);
          }
          if(course.getJsonObject("departmentObject") == null) {
            context.fail("No department object found");
            return;
          }
          if(!course.getJsonObject("departmentObject").getString("id").equals(DEPARTMENT_1_ID)) {
            context.fail("Bad id for department object, got " +
                course.getJsonObject("departmentObject").getString("id") +
                " expected " + DEPARTMENT_1_ID);
          }
          if(courseListing.getJsonObject("courseTypeObject") == null) {
            context.fail("No course type object found in json " + course.encode());
            return;
          }
          if(!courseListing.getJsonObject("courseTypeObject").getString("id").equals(COURSE_TYPE_1_ID)) {
            context.fail("Bad id for course type object, got " +
                courseListing.getJsonObject("courseTypeId").getString("id") +
                " expected " + COURSE_TYPE_1_ID);
          }
          JsonArray instructorObjects = courseListing.getJsonArray("instructorObjects");
          if(instructorObjects == null) {
            context.fail("No instructor objects found in courselisting");
            return;
          }
          if(instructorObjects.size() < 2) {
            context.fail("Expected at least two instructor objects in courselisting: " + courseListing.encode());
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
        async.complete();
      }
    });
  }

  @Test
  public void putCourseListingById(TestContext context) {
    Async async = context.async();
    CaseInsensitiveHeaders acceptText = new CaseInsensitiveHeaders();
    acceptText.add("Accept", "text/plain");
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_1_ID)
        .put("termId", TERM_2_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString());
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID,
        PUT, acceptText, courseListingJson.encode(), 204, "Put CourseListing 1")
        .setHandler( res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
            , GET, null, null, 200, "Get courselisting by id").setHandler(
                res2 -> {
          if(res2.failed()) {
            context.fail(res2.cause());
          } else {
            try {
              JsonObject courseListing = res2.result().getJson();
              if(!courseListing.getString("termId").equals(TERM_2_ID)) {
                context.fail("Bad term id for courselisting after put");
                return;
              }
            } catch(Exception e) {
              context.fail(e);
            }
            async.complete();
          }
        });
      }
    });
  }

  @Test
  public void postInstructorToCourseListing(TestContext context) {
    Async async = context.async();
    JsonObject instructorJson = new JsonObject()
        .put("id", UUID.randomUUID().toString())
        .put("name", "Stainless Steel Rat")
        .put("userId", OkapiMock.user1Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID + "/instructors",
        POST, standardHeaders, instructorJson.encode(), 201, "Post Instructor to Course Listing")
        .setHandler( postRes -> {
          if(postRes.failed()) {
            context.fail(postRes.cause());
          } else {
            TestUtil.doRequest(vertx, baseUrl + "/courselistings/"
              + COURSE_LISTING_1_ID + "/instructors/" + instructorJson.getString("id"),
              GET, standardHeaders, null, 200, "Get instructor from courselisting by id").setHandler(
                getRes -> {
              if(getRes.failed()) {
                context.fail(getRes.cause());
              } else {
                try {
                  JsonObject returnedInstructorJson = getRes.result().getJson();
                  if(!returnedInstructorJson.getString("id").equals(instructorJson.getString("id"))) {
                    context.fail("Returned instructor does not match that which was POSTed");
                    return;
                  }
                  if(!returnedInstructorJson.getJsonObject("patronGroupObject")
                      .getString("id").equals(OkapiMock.group1Id)) {
                    context.fail("Expected id '" + OkapiMock.group1Id + "' for patron group");
                    return;
                  }
                } catch(Exception e) {
                  context.fail(e);
                }
                async.complete();
              }
            });
          }
        });
  }

  @Test
  public void getUser(TestContext context) {
    Async async = context.async();
    String userId = OkapiMock.user1Id;
    TestUtil.doRequest(vertx, okapiUrl + "/users/" + userId, GET, null, null,
        200, "Get user from mock okapi").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject userJson = res.result().getJson();
          if(!userJson.getString("id").equals(userId)) {
            context.fail("Returned user id does not match " + userId);
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getGroup(TestContext context) {
    Async async = context.async();
    String groupId = OkapiMock.group1Id;
    TestUtil.doRequest(vertx, okapiUrl + "/groups/" + groupId, GET, null, null,
        200, "Get group from mock okapi").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject groupJson = res.result().getJson();
          if(!groupJson.getString("id").equals(groupId)) {
            context.fail("Returned user id does not match " + groupId);
            return;
          }
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
      }
    });
  }

  @Test
  public void getPatronGroupFromUserId(TestContext context) {
    Async async = context.async();
    CRUtil.lookupPatronGroupByUserId(OkapiMock.user1Id, okapiHeaders,
        vertx.getOrCreateContext()).setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        PatronGroupObject patronGroupObject = res.result();
        if(!patronGroupObject.getId().equals(OkapiMock.group1Id)) {
          context.fail("Retrieved Group ID does not match");
          return;
        }
        async.complete();
      }
    });
  }
  
  /* UTILITY CLASSES */

   private Future<Void> loadCourseListing1Instructor1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", INSTRUCTOR_1_ID)
        .put("name", "Blaufarb")
        .put("userId", OkapiMock.user2Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID +
        "/instructors", POST, standardHeaders,
        departmentJson.encode(), 201, "Post Instructor 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }
   
  private Future<Void> loadCourseListing1Instructor2() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", INSTRUCTOR_2_ID)
        .put("name", "Kregley")
        .put("userId", OkapiMock.user3Id)
        .put("courseListingId", COURSE_LISTING_1_ID);
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/" + COURSE_LISTING_1_ID
        +"/instructors", POST, standardHeaders,
        departmentJson.encode(), 201, "Post Instructor 2").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadDepartment1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", DEPARTMENT_1_ID)
        .put("name", "History");
    TestUtil.doRequest(vertx, baseUrl + "/departments", POST, null,
        departmentJson.encode(), 201, "Post Department 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadTerm1() {
    Future<Void> future = Future.future();
    DateTime startDate = new DateTime(2019, 6, 15, 0, 0);
    DateTime endDate = new DateTime(2019, 12, 15, 0, 0);
    JsonObject termJson = new JsonObject()
        .put("id", TERM_1_ID)
        .put("name", "Term 1")
        .put("startDate", startDate.toString(ISODateTimeFormat.dateTime()))
        .put("endDate", endDate.toString(ISODateTimeFormat.dateTime()));
    TestUtil.doRequest(vertx, baseUrl + "/terms", POST, null,
        termJson.encode(), 201, "Post Term 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadTerm2() {
    Future<Void> future = Future.future();
    DateTime startDate = new DateTime(2019, 11, 5, 0, 0);
    DateTime endDate = new DateTime(2020, 01, 15, 0, 0);
    JsonObject termJson = new JsonObject()
        .put("id", TERM_2_ID)
        .put("name", "Term 2")
        .put("startDate", startDate.toString(ISODateTimeFormat.dateTime()))
        .put("endDate", endDate.toString(ISODateTimeFormat.dateTime()));
    TestUtil.doRequest(vertx, baseUrl + "/terms", POST, null,
        termJson.encode(), 201, "Post Term 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseType1() {
    Future<Void> future = Future.future();
    JsonObject departmentJson = new JsonObject()
        .put("id", COURSE_TYPE_1_ID)
        .put("name", "Regular");
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", POST, null,
        departmentJson.encode(), 201, "Post Course Type 1").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourseListing1() {
    Future<Void> future = Future.future();
    JsonObject courseListingJson = new JsonObject()
        .put("id", COURSE_LISTING_1_ID)
        .put("termId", TERM_1_ID)
        .put("courseTypeId", COURSE_TYPE_1_ID)
        .put("externalId", UUID.randomUUID().toString());
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", POST, null,
        courseListingJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse1() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_1_ID)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("name", "Comp Sci 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> loadCourse2() {
    Future<Void> future = Future.future();
    JsonObject courseJson = new JsonObject()
        .put("id", COURSE_2_ID)
        .put("departmentId", DEPARTMENT_1_ID)
        .put("courseListingId", COURSE_LISTING_1_ID)
        .put("name", "Comp Eng 101");
    TestUtil.doRequest(vertx, baseUrl + "/courses", POST, null,
        courseJson.encode(), 201, "Post Course Listing").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourses() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courses", DELETE, null, null, 204,
        "Delete All Courses").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseListings() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings", DELETE, null, null, 204,
        "Delete All Course Listings").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteTerms() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/terms", DELETE, null, null, 204,
        "Delete All Terms").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteDepartments() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/departments", DELETE, null, null, 204,
        "Delete All Departments").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

  private Future<Void> deleteCourseTypes() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/coursetypes", DELETE, null, null, 204,
        "Delete All Course Types").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

    private Future<Void> deleteCourseListing1Instructors() {
    Future<Void> future = Future.future();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/"+COURSE_LISTING_1_ID+
        "/instructors", DELETE, null, null, 204,
        "Delete All Instructors").setHandler(res -> {
          if(res.failed()) {
           future.fail(res.cause());
          } else {
            future.complete();
          }
        });
    return future;
  }

}
