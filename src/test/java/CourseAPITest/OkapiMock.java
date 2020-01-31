package CourseAPITest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.http.HttpServer;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class OkapiMock extends AbstractVerticle {
  private final Logger logger = LoggerFactory.getLogger(OkapiMock.class.getClass()
      .getName());
  public static String user1Id = UUID.randomUUID().toString();
  public static String user2Id = UUID.randomUUID().toString();
  public static String user3Id = UUID.randomUUID().toString();
  public static String group1Id = UUID.randomUUID().toString();
  public static String group2Id = UUID.randomUUID().toString();
  public static String group3Id = UUID.randomUUID().toString();
  public static String item1Id = UUID.randomUUID().toString();
  public static String item2Id = UUID.randomUUID().toString();
  public static String holdings1Id = UUID.randomUUID().toString();
  public static String instance1Id = UUID.randomUUID().toString();
  public static String barcode1 = "326547658598";
  public static String barcode2 = "539311253355";
  public static String barcode3 = "794630622287";
  public static String location1Id = UUID.randomUUID().toString();
  public static String location2Id = UUID.randomUUID().toString();
  public static String callNumber1 = "D15.H63 A3 2002";
  public static String uri1 = "http://something.something";
  public static String volume1 = "1";
  public static String enumeration1 = "one";
  public static String title1 = "Interesting Times";
  public static String copy1 = "one";
  public static String contributor1Name = "Einstein, Albert";
  public static String contributor1TypeText = "blahblah";
  public static String contributor1TypeId = UUID.randomUUID().toString();
  public static String contributor1NameTypeId = UUID.randomUUID().toString();
  public static String publication1Publisher = "Random House";
  public static String publication1Place = "London";
  public static String publication1Date = "2011-11-11T03:34:45.823+0000";
  public static String publication1Role = "Publisher";
  public static String servicePoint1Id = UUID.randomUUID().toString();
  public static String servicePoint2Id = UUID.randomUUID().toString();
  public static String servicePoint3Id = UUID.randomUUID().toString();
  public static String library1Id = UUID.randomUUID().toString();
  public static String institution1Id = UUID.randomUUID().toString();
  public static String campus1Id = UUID.randomUUID().toString();
  public static String staffSlip1Id = UUID.randomUUID().toString();
  public static String loanType1Id = UUID.randomUUID().toString();

  private static Map<String, JsonObject> userMap = new HashMap<>();
  private static Map<String, JsonObject> groupMap = new HashMap<>();
  private static Map<String, JsonObject> itemMap = new HashMap<>();
  private static Map<String, JsonObject> holdingsMap = new HashMap<>();
  private static Map<String, JsonObject> instanceMap = new HashMap<>();
  private static Map<String, JsonObject> locationMap = new HashMap<>();
  private static Map<String, JsonObject> servicePointMap = new HashMap<>();
  private static Map<String, JsonObject> loanTypeMap = new HashMap<>();

  public void start(Future<Void> future) {
    final String defaultPort = context.config().getInteger("port", 9130).toString();
    final String portStr = System.getProperty("port", defaultPort);
    final int port = Integer.parseInt(portStr);

    initData();

    Router router = Router.router(vertx);
    HttpServer server = vertx.createHttpServer();

    router.route("/*").handler(BodyHandler.create());
    router.route("/users/:id").handler(this::handleUsers);
    router.route("/groups/:id").handler(this::handleGroups);
    router.route("/item-storage/items/:id").handler(this::handleItems);
    router.route("/holdings-storage/holdings/:id").handler(this::handleHoldings);
    router.route("/instance-storage/instances/:id").handler(this::handleInstances);
    router.route("/locations/:id").handler(this::handleLocations);
    router.route("/service-points/:id").handler(this::handleServicePoints);
    router.route("/loan-types/:id").handler(this::handleLoanTypes);

    logger.info("Running OkapiMock on port " + port);
    server.requestHandler(router::accept).listen(port, result -> {
      if(result.failed()) {
        future.fail(result.cause());
      } else {
        future.complete();
      }
    });
  }

   private void handleUsers(RoutingContext context) {
      String id = context.request().getParam("id");
      if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(userMap.containsKey(id)) {
            context.response().setStatusCode(200).end(userMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }

   private void handleGroups(RoutingContext context) {
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(groupMap.containsKey(id)) {
            context.response().setStatusCode(200).end(groupMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }
   
   private void handleItems(RoutingContext context) {
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(itemMap.containsKey(id)) {
            context.response().setStatusCode(200).end(itemMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }
   
   private void handleHoldings(RoutingContext context) {
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(holdingsMap.containsKey(id)) {
            context.response().setStatusCode(200).end(holdingsMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }
   
   private void handleInstances(RoutingContext context) {
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(instanceMap.containsKey(id)) {
            context.response().setStatusCode(200).end(instanceMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }
   
   private void handleLocations(RoutingContext context) {
     logger.info("Got location request");
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(locationMap.containsKey(id)) {
            context.response().setStatusCode(200).end(locationMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }
   
   private void handleServicePoints(RoutingContext context) {
     logger.info("Got service points request");
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(servicePointMap.containsKey(id)) {
            context.response().setStatusCode(200).end(servicePointMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }

   private void handleLoanTypes(RoutingContext context) {
     logger.info("Got loan types request");
     String id = context.request().getParam("id");
     if(context.request().method() == HttpMethod.GET) {
        if(id == null) {
          String message = String.format("List retrieval currently unsupported");
          context.response().setStatusCode(400)
          .end(message);
          return;
        } else {
          if(loanTypeMap.containsKey(id)) {
            context.response().setStatusCode(200).end(loanTypeMap.get(id).encode());
            return;
          } else {
            context.response().setStatusCode(404).end("id '" + id + "' not found");
          }
        }
      } else {
        String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
      }
   }

   private void initData() {
    userMap.put(user1Id, new JsonObject()
      .put("id", user1Id)
      .put("username", "elsanto")
      .put("patronGroup", group1Id)
      .put("barcode", barcode1 )
      .put("active", Boolean.TRUE));

    userMap.put(user2Id, new JsonObject()
      .put("id", user2Id)
      .put("username", "grasso")
      .put("patronGroup", group3Id)
      .put("barcode", barcode2)
      .put("active", Boolean.TRUE));

    userMap.put(user3Id, new JsonObject()
      .put("id", user3Id)
      .put("username", "lews")
      .put("patronGroup", group2Id)
      .put("barcode", barcode3)
      .put("active", Boolean.TRUE));

    groupMap.put(group1Id, new JsonObject()
      .put("id", group1Id)
      .put("name", "wrasslers")
      .put("desc", "People Who Wrassle")
    );

    groupMap.put(group2Id, new JsonObject()
      .put("id", group2Id)
      .put("name", "workers")
      .put("desc", "People Who Work")
    );

    groupMap.put(group3Id, new JsonObject()
      .put("id", group3Id)
      .put("name", "managers")
      .put("desc", "People Who manage")
    );

    itemMap.put(item1Id, new JsonObject()
        .put("id", item1Id)
        .put("status", "Available")
        .put("holdingsRecordId", holdings1Id)
        .put("barcode", barcode1)
        .put("volume", volume1)
        .put("enumeration", enumeration1)
        .put("copyNumber", copy1)
        .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
            .put("uri", uri1)
            .put("publicNote", uri1))
          )

    );

    itemMap.put(item2Id, new JsonObject()
      .put("id", item2Id)
      .put("status", "Available")
      .put("holdingsRecordId", holdings1Id)
      .put("barcode", barcode2)
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
      .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
             .put("uri", uri1)
            .put("publicNote", uri1))
          )
    );

    holdingsMap.put(holdings1Id, new JsonObject()
      .put("id", holdings1Id)
      .put("instanceId", instance1Id)
      .put("permanentLocationId", location1Id)
      .put("temporaryLocationId", location2Id)
      .put("callNumber", callNumber1)
      
    );

    instanceMap.put(instance1Id, new JsonObject()
      .put("id", instance1Id)
      .put("title", title1)
      .put("contributors", new JsonArray()
        .add(new JsonObject()
          .put("contributorTypeId", contributor1TypeId)
          .put("name", contributor1Name)
          .put("contributorTypeText", contributor1TypeText)
          .put("contributorNameTypeId", contributor1NameTypeId)
          .put("primary", true)
        )
      )
      .put("publication", new JsonArray()
        .add(new JsonObject()
          .put("publisher", publication1Publisher)
          .put("place", publication1Place)
          .put("dateOfPublication", publication1Date)
          .put("role", publication1Role)
        )
      )
    );
    
    locationMap.put(location1Id, new JsonObject()
        .put("id", location1Id)
        .put("name", "Location 1")
        .put("discoveryDisplayName", "The Mighty Location One")
        .put("isActive", true)
        .put("code", "loc1")
        .put("institutionId", institution1Id)
        .put("campusId", campus1Id)
        .put("libraryId", library1Id)
        .put("primaryServicePoint", servicePoint1Id)
        .put("servicePointIds", new JsonArray()
          .add(servicePoint1Id)
          .add(servicePoint2Id)
          .add(servicePoint3Id)
        )        
    );

    locationMap.put(location2Id, new JsonObject()
      .put("id", location2Id)
      .put("name", "Location 2")
      .put("discoveryDisplayName", "The Mightly Location Two")
      .put("isActive", true)
      .put("code", "loc2")
      .put("institutionId", institution1Id)
      .put("campusId", campus1Id)
      .put("libraryId", library1Id)
      .put("primaryServicePoint", servicePoint2Id)
      .put("servicePointIds", new JsonArray()
          .add(servicePoint2Id)
          .add(servicePoint3Id)
        )
    );
    
    //TODO: Populate Service Points
    servicePointMap
      .put(servicePoint1Id, new JsonObject()
        .put("id", servicePoint1Id)
        .put("name", "Circ Desk 1")
        .put("code", "cd1")
        .put("discoveryDisplayName", "Circulation Desk -- Main")
        .put("pickupLocation", true)
        .put("holdShelfExpiryPeriod", new JsonObject()
          .put("duration", 5)
          .put("intervalId", "Days")
        )
        .put("staffSlips", new JsonArray()
          .add(new JsonObject()
            .put("id", staffSlip1Id)
            .put("printByDefault", true)
          )
          .add(new JsonObject()
            .put("id", UUID.randomUUID().toString())
            .put("printByDefault", true)
          )
          .add(new JsonObject()
            .put("id", UUID.randomUUID().toString())
            .put("printByDefault", true)
          )
        )
    );
    
    servicePointMap.put(servicePoint2Id, new JsonObject()
      .put("id", servicePoint2Id)
      .put("name", "Circ Desk 2")
      .put("code", "cd2")
      .put("discoveryDisplayName", "Circulation Desk -- Secondary")
      .put("pickupLocation", true)
      .put("holdShelfExpiryPeriod", new JsonObject()
        .put("duration", 5)
        .put("intervalId", "Days")
      )
      .put("staffSlips", new JsonArray()
        .add(new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("printByDefault", true)
        )
        .add(new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("printByDefault", true)
        )
        .add(new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("printByDefault", true)
        )
      )
    );
    servicePointMap.put(servicePoint3Id, new JsonObject()
      .put("id", servicePoint3Id)
      .put("name", "Library Service Desk")
      .put("code", "lsd")
      .put("discoveryDisplayName", "Yet Another Library Service Desk")
      .put("pickupLocation", true)
      .put("holdShelfExpiryPeriod", new JsonObject()
        .put("duration", 7)
        .put("intervalId", "Days")
      )
      .put("staffSlips", new JsonArray()
        .add(new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("printByDefault", true)
        )
        .add(new JsonObject()
          .put("id", UUID.randomUUID().toString())
          .put("printByDefault", true)
        )
      )
    );

    loanTypeMap.put(loanType1Id, new JsonObject()
      .put("id", loanType1Id)
      .put("name", "Reserved Loan")
    );
        
  }
}
