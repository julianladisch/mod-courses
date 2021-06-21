package CourseAPITest;

import static CourseAPITest.CourseAPITest.MODULE_FROM;
import static CourseAPITest.CourseAPITest.MODULE_TO;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.MultiMap;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
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
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.HttpResponse;
import io.vertx.ext.web.client.WebClient;
import java.util.HashMap;
import java.util.Map;
import org.folio.coursereserves.util.CRUtil;
import org.folio.postgres.testing.PostgresTesterContainer;
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
  protected static Vertx vertx;
  protected static final Logger logger = LoggerFactory.getLogger(CourseAPIWithSampleDataTest.class);
  public static String baseUrl;
  public static String okapiUrl;
  public static String okapiTenantUrl;
  public static Map<String, String> okapiHeaders = new HashMap<>();
  public static MultiMap standardHeaders = MultiMap.caseInsensitiveMultiMap();
  public static MultiMap acceptTextHeaders = MultiMap.caseInsensitiveMultiMap();
  protected static String restVerticleId;
  protected static String okapiVerticleId;

  @Rule
  public Timeout rule = Timeout.seconds(200);

  @BeforeClass
  public static void beforeClass(TestContext context) {
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
    PostgresClient.setPostgresTester(new PostgresTesterContainer());
    vertx.deployVerticle(OkapiMock.class.getName(), okapiOptions)
    .onSuccess(id -> okapiVerticleId = id)
    .onSuccess(x -> logger.info("Deployed Mock Okapi on port " + okapiPort))
    .compose(x-> vertx.deployVerticle(RestVerticle.class.getName(), options))
    .onSuccess(id -> restVerticleId = id)
    .compose(x -> resetMockOkapi())
    .compose(x -> addSampleData())
    .onSuccess(x -> logger.info("Deployed verticle on port " + port))
    .compose(x -> initTenant("diku", port))
    .onComplete(context.asyncAssertSuccess());
  }

  @AfterClass
  public static void afterClass(TestContext context) {
    vertx.undeploy(okapiVerticleId)
    .compose(x -> vertx.undeploy(restVerticleId))
    .onComplete(context.asyncAssertSuccess());
  }


  @Before
  public void beforeEach(TestContext context) {
    Async async = context.async();
    resetMockOkapi().onComplete(res -> {
      addSampleData().onComplete(res2 -> {
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

  protected static Future<Void> addSampleData() {
    Promise<Void> promise = Promise.promise();
    JsonObject payload = new JsonObject().put("add", true);
    logger.info("Making request to add sample mock okapi data");
    CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/addsample", POST, null,
        payload.encode(), 201).onComplete(res -> {
      if(res.failed()) {
        promise.fail(res.cause());
      } else {
        promise.complete();
      }
    });
    return promise.future();
  }

  protected static Future<Void> resetMockOkapi() {
    Promise<Void> promise = Promise.promise();
    JsonObject payload = new JsonObject().put("reset", true);
    logger.info("Making request to reset mock okapi data");
    CRUtil.makeOkapiRequest(vertx, okapiHeaders, "/reset", POST, null,
        payload.encode(), 201).onComplete(res -> {
      if(res.failed()) {
        promise.fail(res.cause());
      } else {
        promise.complete();
      }
    });
    return promise.future();
  }

  protected static Future<Void> initTenant(String tenantId, int port) {
    Promise<Void> promise = Promise.promise();
    WebClient client = WebClient.create(vertx);
    String url = "http://localhost:" + port + "/_/tenant";
    JsonObject payload = new JsonObject()
        .put("module_to", MODULE_TO)
        .put("module_from", MODULE_FROM)
        .put("parameters", new JsonArray()
          .add(new JsonObject()
            .put("key", "loadSample")
            .put("value", true))
         );
    HttpRequest<Buffer> request = client.postAbs(url);
    request.putHeader("X-Okapi-Tenant", tenantId);
    request.putHeader("X-Okapi-Url", okapiUrl);
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Accept", "application/json, text/plain");
    request.putHeader("X-Okapi-Url-To", okapiTenantUrl);
    request.sendJsonObject(payload).onComplete(res -> {
      if(res.failed()) {
        promise.fail(res.cause());
      } else {
        HttpResponse<Buffer> result = res.result();
        if(result.statusCode() != 204) {
          promise.fail("Expected 204, got " + result.statusCode());
        } else {
          promise.complete();
        }
      }
    });
    return promise.future();
  }

  //Tests

  @Test
  public void dummyTest(TestContext context) {
    Async async = context.async();
    context.assertTrue(true);
    async.complete();
  }

  @Test
  public void testCourseListingLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/cef52efb-b3fd-4450-9960-1745026a99d1",
        GET, standardHeaders, null, 200, "Get CourseListing").onComplete(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  public void testReserveLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/reserves/67227d94-7333-4d22-98a0-718b49d36595",
        GET, standardHeaders, null, 200, "Get Reserve").onComplete(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }


}


