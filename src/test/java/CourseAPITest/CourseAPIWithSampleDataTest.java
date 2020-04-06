package CourseAPITest;

import static CourseAPITest.CourseAPITest.MODULE_FROM;
import static CourseAPITest.CourseAPITest.MODULE_TO;
import static CourseAPITest.CourseAPITest.port;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import static io.vertx.core.http.HttpMethod.GET;
import static io.vertx.core.http.HttpMethod.POST;
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
import org.folio.coursereserves.util.CRUtil;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CourseAPIWithSampleDataTest {
  static int port;
  static int okapiPort;
  private static Vertx vertx;
  private static final Logger logger = LoggerFactory.getLogger(CourseAPIWithSampleDataTest.class);
  public static String baseUrl;
  public static String okapiUrl;
  public static String okapiTenantUrl;
  public static Map<String, String> okapiHeaders = new HashMap<>();
  public static CaseInsensitiveHeaders standardHeaders = new CaseInsensitiveHeaders();
  public static CaseInsensitiveHeaders acceptTextHeaders = new CaseInsensitiveHeaders();
  private static String restVerticleId;
  private static String okapiVerticleId;

  @Rule
  public Timeout rule = Timeout.seconds(200);

  @BeforeClass
  public static void beforeClass(TestContext context) {
    Async async = context.async();
    port = NetworkUtils.nextFreePort();
    okapiPort = NetworkUtils.nextFreePort();
    baseUrl = "http://localhost:"+port+"/coursereserves";
    okapiUrl = "http://localhost:"+okapiPort;
    //okapiUrl = "http://localhost:" + port;
    okapiTenantUrl = "http://localhost:" + port;
    //TenantClient tenantClient = new TenantClient("localhost", port, "diku", "diku");
    okapiHeaders.put("x-okapi-tenant", "diku");
    okapiHeaders.put("x-okapi-url", okapiUrl);
    standardHeaders.add("x-okapi-url", okapiUrl);
    acceptTextHeaders.add("accept", "text/plain");
    acceptTextHeaders.add("x-okapi-url", okapiUrl);
    vertx = Vertx.vertx();
    DeploymentOptions options = new DeploymentOptions()
        .setConfig(new JsonObject().put("http.port", port));
    DeploymentOptions okapiOptions = new DeploymentOptions()
        .setConfig(new JsonObject().put("port", okapiPort));
    try {
      PostgresClient.setEmbeddedPort(NetworkUtils.nextFreePort());
      PostgresClient.setIsEmbedded(true);
      PostgresClient.getInstance(vertx).startEmbeddedPostgres();
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
      return;
    }
    vertx.deployVerticle(OkapiMock.class.getName(), okapiOptions, deployOkapiRes -> {
      if(deployOkapiRes.failed()) {
        context.fail(deployOkapiRes.cause());
      } else {
        okapiVerticleId = deployOkapiRes.result();
        logger.info("Deployed Mock Okapi on port " + okapiPort);
        vertx.deployVerticle(RestVerticle.class.getName(), options, deployCourseRes -> {
          if(deployCourseRes.failed()) {
            context.fail(deployCourseRes.cause());
          } else {
            resetMockOkapi().compose(f -> {
              return addSampleData();
            }).setHandler(res -> {
              try {
                restVerticleId = deployCourseRes.result();
                logger.info("Deployed verticle on port " + port);
                initTenant("diku", port).setHandler(initRes -> {
                  if(initRes.failed()) {
                    context.fail(initRes.cause());
                  } else {
                    async.complete();
                  }
                });
              } catch(Exception e) {
                e.printStackTrace();
                context.fail(e);
              }
            });
          }
        });
      }
    });
  }

  @AfterClass
  public static void afterClass(TestContext context) {
    Async async = context.async();
    vertx.undeploy(okapiVerticleId, undeployOkapiRes -> {
      if(undeployOkapiRes.failed()) {
        context.fail(undeployOkapiRes.cause());
      } else {
        vertx.undeploy(restVerticleId, undeployCourseRes -> {
          if(undeployCourseRes.failed()) {
            context.fail(undeployCourseRes.cause());
          } else {
            PostgresClient.stopEmbeddedPostgres();
            async.complete();
            /*
            vertx.close(context.asyncAssertSuccess( res -> {
              PostgresClient.stopEmbeddedPostgres();
              try {
                Thread.sleep(3000);
              } catch(Exception e) {
                logger.error(e.getLocalizedMessage());
              }
              async.complete();
            }));
            */
          }
        });
      }
    });
  }


  @Before
  public void beforeEach(TestContext context) {
    Async async = context.async();
    resetMockOkapi().setHandler(res -> {
      addSampleData().setHandler(res2 -> {
        if(res2.failed()) {
          context.fail(res2.cause());
        } else {
          async.complete();
        }
      });
    });
  }

  @After
  public void afterEach(TestContext context) {
    Async async = context.async();
    logger.info("After each");
    async.complete();
  }

  private static Future<Void> addSampleData() {
    Future<Void> future = Future.future();
    JsonObject payload = new JsonObject().put("add", true);
    logger.info("Making request to add sample mock okapi data");
    CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/addsample", POST, null,
        payload.encode(), 201).setHandler(res -> {
      if(res.failed()) {
        future.fail(res.cause());
      } else {
        future.complete();
      }
    });
    return future;
  }

  private static Future<Void> resetMockOkapi() {
    Future<Void> future = Future.future();
    JsonObject payload = new JsonObject().put("reset", true);
    logger.info("Making request to reset mock okapi data");
    CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/reset", POST, null,
        payload.encode(), 201).setHandler(res -> {
      if(res.failed()) {
        future.fail(res.cause());
      } else {
        future.complete();
      }
    });
    return future;
  }

  private static Future<Void> initTenant(String tenantId, int port) {
    Promise<Void> promise = Promise.promise();
    HttpClient client = vertx.createHttpClient();
    String url = "http://localhost:" + port + "/_/tenant?tenantParameters=loadSample=true";
    JsonObject payload = new JsonObject()
        .put("module_to", MODULE_TO)
        .put("module_from", MODULE_FROM)
        .put("parameters", new JsonArray()
          .add(new JsonObject()
            .put("key", "loadSample")
            .put("value", true))
         );
    HttpClientRequest request = client.postAbs(url);
    request.handler(req -> {
      if(req.statusCode() != 201) {
        promise.fail("Expected 201, got " + req.statusCode());
      } else {
        promise.complete();
      }
    });
    request.putHeader("X-Okapi-Tenant", tenantId);
    request.putHeader("X-Okapi-Url", okapiUrl);
    request.putHeader("X-Okapi-Url-To", okapiTenantUrl);
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Accept", "application/json, text/plain");
    request.end(payload.encode());
    return promise.future();
  }

  //Tests

  @Test
  public void dummyTest(TestContext context) {
    Async async = context.async();
    async.complete();
  }
  
  @Test
  public void testCourseListingLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/cef52efb-b3fd-4450-9960-1745026a99d1",
        GET, standardHeaders, null, 200, "Get CourseListing").setHandler(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

}


