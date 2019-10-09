package CourseAPITest;

import io.vertx.core.DeploymentOptions;
import io.vertx.core.Vertx;
import static io.vertx.core.http.HttpMethod.GET;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.Timeout;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import javax.ws.rs.HttpMethod;
import org.folio.rest.RestVerticle;
import org.folio.rest.client.TenantClient;
import org.folio.rest.persist.PostgresClient;
import org.folio.rest.tools.utils.NetworkUtils;
import org.junit.AfterClass;
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
        async.complete();
      }
    });
  }
}
