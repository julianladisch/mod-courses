package CourseAPITest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static CourseAPITest.CourseAPITest.MODULE_FROM;
import static CourseAPITest.CourseAPITest.MODULE_TO;
import io.vertx.core.DeploymentOptions;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.buffer.Buffer;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import io.vertx.ext.web.client.HttpRequest;
import io.vertx.ext.web.client.WebClient;
import org.folio.postgres.testing.PostgresTesterContainer;
import org.folio.rest.RestVerticle;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CourseAPINoLoadSampleDataTest extends CourseAPIWithSampleDataTest {


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
    .compose(x -> vertx.deployVerticle(RestVerticle.class.getName(), options))
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
  @Override
  public void beforeEach(TestContext context) {
    resetMockOkapi()
    .compose(x -> addSampleData())
    .onComplete(context.asyncAssertSuccess());
  }

  @After
  @Override
  public void afterEach(TestContext context) {
    logger.info("After each");
  }





  protected static Future<Void> initTenant(String tenantId, int port) {
    WebClient client = WebClient.create(vertx);
    String url = "http://localhost:" + port + "/_/tenant";
    JsonObject payload = new JsonObject()
        .put("module_to", MODULE_TO)
        .put("module_from", MODULE_FROM);
    HttpRequest<Buffer> request = client.postAbs(url);
    request.putHeader("X-Okapi-Tenant", tenantId);
    request.putHeader("X-Okapi-Url", okapiUrl);
    request.putHeader("Content-Type", "application/json");
    request.putHeader("Accept", "application/json, text/plain");
    return request.sendJsonObject(payload)
    .map(result -> {
      assertThat(result.statusCode(), is(204));
      return null;
    });
  }

  //Tests

  @Test
  @Override
  public void testCourseListingLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/courselistings/cef52efb-b3fd-4450-9960-1745026a99d1",
        GET, standardHeaders, null, 404, "Get CourseListing").onComplete(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

  @Test
  @Override
  public void testReserveLoad(TestContext context) {
    Async async = context.async();
    TestUtil.doRequest(vertx, baseUrl + "/reserves/67227d94-7333-4d22-98a0-718b49d36595",
        GET, standardHeaders, null, 404, "Get Reserve").onComplete(res -> {
      if(res.failed()) {
        context.fail(res.cause());
      } else {
        async.complete();
      }
    });
  }

}


