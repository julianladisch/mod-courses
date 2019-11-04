package CourseAPITest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import static io.vertx.core.http.HttpMethod.DELETE;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import javax.ws.rs.HttpMethod;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
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
  private static Vertx vertx;
  private final Logger logger = LoggerFactory.getLogger(CourseAPITest.class);
  public static String baseUrl;
  public final static String COURSE_LISTING_1_ID = UUID.randomUUID().toString();
  public final static String TERM_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_2_ID = UUID.randomUUID().toString();
  public final static String DEPARTMENT_1_ID = UUID.randomUUID().toString();
  public final static String COURSE_TYPE_1_ID = UUID.randomUUID().toString();


  @Rule
  public Timeout rule = Timeout.seconds(200);

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    baseUrl = "http://localhost:"+port+"/coursereserves";
    TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
    vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port));
    try {
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }
    vertx.deployVerticle(RestVerticle.class.getName(), options, res -> {
      try {
        tenantClient.postTenant(null, res2 -> {
          async.complete();
        });
      } catch(Exception e) {
        e.printStackTrace();
        context.fail(e);
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
        return loadCourseListing1();
      })
      .compose(f -> {
        return loadDepartment1();
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
  public void getCourseById(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courses/" + COURSE_1_ID, GET, null, null, 200,
        "Get course by id").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        try {
          JsonObject course = res.result().getJson();
          if(course.getJsonObject("courseListingObject") == null) {
            context.fail("No course listing object found");
            return;
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
          async.complete();
        } catch(Exception e) {
          context.fail(e);
        }
        async.complete();
      }
    });
  }
  /* UTILITY CLASSES */

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

}
