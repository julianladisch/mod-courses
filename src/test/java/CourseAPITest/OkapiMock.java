package CourseAPITest;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Promise;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.util.StringUtil;


public class OkapiMock extends AbstractVerticle {
  private static final Logger logger = LoggerFactory.getLogger(OkapiMock.class.getClass()
      .getName());
  public static String user1Id = UUID.randomUUID().toString();
  public static String user2Id = UUID.randomUUID().toString();
  public static String user3Id = UUID.randomUUID().toString();
  public static String user4Id = UUID.randomUUID().toString();
  public static String group1Id = UUID.randomUUID().toString();
  public static String group2Id = UUID.randomUUID().toString();
  public static String group3Id = UUID.randomUUID().toString();
  public static String item1Id = UUID.randomUUID().toString();
  public static String item2Id = UUID.randomUUID().toString();
  public static String item3Id = UUID.randomUUID().toString();
  public static String item4Id = UUID.randomUUID().toString();
  public static String item5Id = UUID.randomUUID().toString();
  public static String item6Id = UUID.randomUUID().toString();
  /** item where PUT fails */
  public static String item7Id = UUID.randomUUID().toString();
  public static String holdings1Id = UUID.randomUUID().toString();
  public static String holdings2Id = UUID.randomUUID().toString();
  public static String instance1Id = UUID.randomUUID().toString();
  public static String instance1Hrid = "inst000000000006";
  public static String barcode1 = "326547658598";
  public static String barcode2 = "539311253355";
  public static String barcode3 = "794630622287";
  public static String barcode4 = "229842532165";
  /** barcode of item where PUT fails */
  public static String barcode7 = "0";
  public static String location1Id = UUID.randomUUID().toString();
  public static String location2Id = UUID.randomUUID().toString();
  public static String fullCallNumber1 = "D15.H63 A3 2002";
  public static String callNumber1 = "791.43";
  public static String callNumberPrefix1 = "F";
  public static String callNumberSuffix1 = "CAM";
  public static String callNumber2 = "800.23";
  public static String callNumberPrefix2 = "N";
  public static String callNumberSuffix2 = "MOO";
  public static String uri1 = "http://something.something";
  public static String uri2 = "http://somethingelse.somethingelse";
  public static String note1 = "note1";
  public static String note2 = "note2";
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
  public static String loanType2Id = UUID.randomUUID().toString();


  private static Map<String, JsonObject> userMap;
  private static Map<String, JsonObject> groupMap;
  private static Map<String, JsonObject> itemMap;
  private static Map<String, JsonObject> holdingsMap;
  private static Map<String, JsonObject> instanceMap;
  private static Map<String, JsonObject> locationMap;
  private static Map<String, JsonObject> servicePointMap;
  private static Map<String, JsonObject> loanTypeMap;


  public void start(Promise<Void> promise) {
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
    router.route("/item-storage/items").handler(this::handleItems);
    router.route("/holdings-storage/holdings/:id").handler(this::handleHoldings);
    router.route("/instance-storage/instances/:id").handler(this::handleInstances);
    router.route("/locations/:id").handler(this::handleLocations);
    router.route("/service-points/:id").handler(this::handleServicePoints);
    router.route("/loan-types/:id").handler(this::handleLoanTypes);
    router.route("/reset").handler(this::handleReset);
    router.route("/wipe").handler(this::handleWipe);
    router.route("/addsample").handler(this::handleAddSample);

    logger.info("Running OkapiMock on port " + port);
    server.requestHandler(router).listen(port)
    .<Void>mapEmpty()
    .onComplete(promise);
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
    logger.info("Got request for items: " + context.request().absoluteURI());
    String id = context.request().getParam("id");
    if (context.request().method() == HttpMethod.GET) {
      String query = context.request().query();
      if (query != null) {
        String barcode = parseBarcode(query);
        logger.info("Searching for barcode " + barcode);
        JsonArray matchingItems = new JsonArray();
        for(JsonObject json : itemMap.values()) {
          if(json.containsKey("barcode") && json.getString("barcode").equals(barcode)) {
            matchingItems.add(json);
          }
        }
        JsonObject result = new JsonObject()
            .put("items", matchingItems)
            .put("totalRecords", matchingItems.size());
        context.response().setStatusCode(200).end(result.encode());
        return;
      } else if (id == null) {
        String message = String.format("List retrieval currently unsupported");
        context.response().setStatusCode(400)
            .end(message);
        return;
      } else if (itemMap.containsKey(id)) {
        context.response().setStatusCode(200).end(itemMap.get(id).encode());
        return;
      } else {
        context.response().setStatusCode(404).end("id '" + id + "' not found");
      }
    } else if (context.request().method() == HttpMethod.PUT) {
      if(id == null) {
        String message = String.format("PUT requires id in path");
        context.response().setStatusCode(400)
            .end(message);
        return;
      }
      try {
        String putContent = context.getBodyAsString();
        if(putContent == null || putContent.length() == 0) {
          throw new UnsupportedOperationException("No content in PUT body read");
        }
        logger.info("Got body of PUT request " + putContent);
        JsonObject putJson = null;
        if(!itemMap.containsKey(id)) {
          context.response().setStatusCode(404)
              .end("Item with id '" + id + "' does not exist");
          return;
        }
        putJson = new JsonObject(putContent);
        if(!putJson.getString("id").equals(id)) {
          throw new UnsupportedOperationException("id field in json must match id '" + id + "'");
        }
        if (6 != putJson.getInteger("_version")) {  // optimistic locking
          throw new IllegalArgumentException("_version must be 6 in putJson: " + putJson.encodePrettily());
        }
        if ("0".equals(putJson.getString("barcode"))) {
          context.response().setStatusCode(500)
              .end("We've mocked barcode 0 to fail on PUT");
          return;
        }
        logger.info("Writing JSON back to mapping");
        itemMap.put(id, putJson);
        logger.info("Return response");
        context.response().setStatusCode(204).end();
      } catch(UnsupportedOperationException e) {
        logger.error(e.getMessage(), e);
        context.response().setStatusCode(400)
            .end(e.getLocalizedMessage());
        return;
      } catch(Exception e) {
        logger.error(e.getMessage(), e);
        context.response().setStatusCode(500)
            .end(e.getLocalizedMessage());
        return;
      }
    } else if(context.request().method() == HttpMethod.DELETE) {
      if(id == null) {
        String message = String.format("DELETE requires id in path");
        context.response().setStatusCode(400)
            .end(message);
      } else {
        try {
          if(!itemMap.containsKey(id)) {
            context.response().setStatusCode(404)
                .end("Item with id '" + id + "' does not exist");
            return;
          } else {
            itemMap.remove(id);
            context.response().setStatusCode(204)
                .end();
          }
        } catch(Exception e) {
          context.response().setStatusCode(500)
              .end(e.getLocalizedMessage());
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

   private static String parseBarcode(String query) {
     Pattern pattern = Pattern.compile("barcode==\"(\\d+)\"");
     Matcher matcher = pattern.matcher(StringUtil.urlDecode(query));
     if(matcher.find()) {
       return matcher.group(1);
     } else {
       return null;
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

   private void handleWipe(RoutingContext context) {
     logger.info("Got wipe request");
     if(context.request().method() == HttpMethod.POST) {
       String postContent = context.getBodyAsString();
       JsonObject json = new JsonObject(postContent);
       try {
        if(json != null && json.containsKey("wipe")) {
          if(json.getBoolean("wipe")) {
            wipeData();
            context.response().setStatusCode(201).end(json.encode());
          } else {
            throw new Exception("Bad input");
          }
        }
       } catch(Exception e) {
         context.response().setStatusCode(400).end(e.getLocalizedMessage());
       }
     } else {
       String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
     }
   }

   private void handleReset(RoutingContext context) {
     logger.info("Got reset request");
     if(context.request().method() == HttpMethod.POST) {
       String postContent = context.getBodyAsString();
       JsonObject json = new JsonObject(postContent);
       try {
        if(json != null && json.containsKey("reset")) {
          if(json.getBoolean("reset")) {
            initData();
            context.response().setStatusCode(201).end(json.encode());
          } else {
            throw new Exception("Bad input");
          }
        }
       } catch(Exception e) {
         context.response().setStatusCode(400).end(e.getLocalizedMessage());
       }
     } else {
       String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
     }
   }

   private void handleAddSample(RoutingContext context) {
     logger.info("Adding sample data in mock okapi");
     if(context.request().method() == HttpMethod.POST) {
       String postContent = context.getBodyAsString();
       JsonObject json = new JsonObject(postContent);
       try {
        if(json != null && json.containsKey("add")) {
          if(json.getBoolean("add")) {
            addSampleData();
            context.response().setStatusCode(201).end(json.encode());
          } else {
            throw new Exception("Bad input");
          }
        }
       } catch(Exception e) {
         context.response().setStatusCode(400).end(e.getLocalizedMessage());
       }
     } else {
       String message = String.format("Unsupported method %s", context.request()
            .method().toString());
        context.response().setStatusCode(400)
          .end(message);
        return;
     }
   }

  private static void wipeData() {
    logger.info("Wiping sample data from mock okapi");
    userMap = new HashMap<>();
    groupMap = new HashMap<>();
    itemMap = new HashMap<>();
    holdingsMap = new HashMap<>();
    instanceMap = new HashMap<>();
    locationMap = new HashMap<>();
    servicePointMap = new HashMap<>();
    loanTypeMap = new HashMap<>();
  }


  private static void initData() {
    logger.info("Resetting data in mock okapi");
    wipeData();
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

    userMap.put(user4Id, new JsonObject()
      .put("id", user4Id)
      .put("username", "snark")
      .put("patronGroup", group2Id)
      .put("barcode", barcode4)
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
        .put("_version", 6)
        .put("status", new JsonObject().put("name", "Available"))
        .put("holdingsRecordId", holdings1Id)
        .put("barcode", barcode1)
        .put("itemLevelCallNumber", callNumber1)
        .put("itemLevelCallNumberPrefix", callNumberPrefix1)
        .put("itemLevelCallNumberSuffix", callNumberSuffix1)
        .put("volume", volume1)
        .put("enumeration", enumeration1)
        .put("copyNumber", copy1)
        .put("permanentLocationId", location1Id)
        .put("temporaryLocationId", location2Id)
        .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
            .put("uri", uri1)
            .put("publicNote", uri1))
          )
    );

    itemMap.put(item2Id, new JsonObject()
      .put("id", item2Id)
      .put("_version", 6)
      .put("status", new JsonObject().put("name", "Available"))
      .put("holdingsRecordId", holdings1Id)
      .put("barcode", barcode2)
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("permanentLocationId", location2Id)
      .put("temporaryLocationId", location1Id)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
      .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
             .put("uri", uri1)
            .put("publicNote", note1))
          )
    );

    itemMap.put(item3Id, new JsonObject()
      .put("id", item3Id)
      .put("_version", 6)
      .put("status", new JsonObject().put("name", "Available"))
      .put("holdingsRecordId", holdings2Id)
      .put("barcode", barcode3)
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("permanentLocationId", location2Id)
      .put("temporaryLocationId", location1Id)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
    );

    itemMap.put(item4Id, new JsonObject()
      .put("id", item4Id)
      .put("_version", 6)
      .put("status", new JsonObject().put("name", "Available"))
      .put("holdingsRecordId", holdings2Id)
      .put("barcode", barcode4)
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("permanentLocationId", location2Id)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
    );


    itemMap.put(item7Id, itemMap
        .get(item1Id).copy()
        .put("id", item7Id)
        .put("barcode", barcode7)
    );


    holdingsMap.put(holdings1Id, new JsonObject()
      .put("id", holdings1Id)
      .put("instanceId", instance1Id)
      .put("permanentLocationId", location1Id)
      .put("temporaryLocationId", location2Id)
      .put("callNumber", fullCallNumber1)
    );

    holdingsMap.put(holdings2Id, new JsonObject()
      .put("id", holdings2Id)
      .put("instanceId", instance1Id)
      .put("permanentLocationId", location1Id)
      .put("temporaryLocationId", location2Id)
      .put("callNumber", callNumber2)
      .put("callNumberSuffix", callNumberSuffix2)
      .put("callNumberPrefix", callNumberPrefix2)
      .put("electronicAccess", new JsonArray()
          .add(new JsonObject()
             .put("uri", uri2)
             .put("publicNote", note2))
          )
    );

    instanceMap.put(instance1Id, new JsonObject()
      .put("id", instance1Id)
      .put("hrid", instance1Hrid)
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


  private static void addSampleData() {
    logger.info("Adding sample data to mock okapi");
    itemMap.put(item5Id, new JsonObject()
      .put("id", item5Id)
      .put("_version", 6)
      .put("status", new JsonObject().put("name", "Available"))
      .put("holdingsRecordId", holdings2Id)
      .put("barcode", "4539876054383" )
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("permanentLocationId", location2Id)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
    );

    itemMap.put(item6Id, new JsonObject()
      .put("id", item6Id)
      .put("_version", 6)
      .put("status", new JsonObject().put("name", "Available"))
      .put("holdingsRecordId", holdings2Id)
      .put("barcode", "90000")
      .put("volume", volume1)
      .put("enumeration", enumeration1)
      .put("permanentLocationId", location2Id)
      .put("copyNumbers", new JsonArray()
          .add(copy1))
    );

     userMap.put("9cc888e5-f6d7-4709-b113-3040e8fbe648", new JsonObject()
      .put("id", "9cc888e5-f6d7-4709-b113-3040e8fbe648")
      .put("username", "maagard")
      .put("patronGroup", group3Id)
      .put("barcode", barcode2)
      .put("active", Boolean.TRUE));

    userMap.put("2e53ca2f-9bd9-424d-bcef-67f5f268edb0", new JsonObject()
      .put("id", "2e53ca2f-9bd9-424d-bcef-67f5f268edb0")
      .put("username", "caadams")
      .put("patronGroup", group2Id)
      .put("barcode", barcode3)
      .put("active", Boolean.TRUE));

    userMap.put("f61c6a9e-92b5-470c-8463-6494afd108e6", new JsonObject()
      .put("id", "f61c6a9e-92b5-470c-8463-6494afd108e6")
      .put("username", "mtaylor")
      .put("patronGroup", group2Id)
      .put("barcode", barcode4)
      .put("active", Boolean.TRUE));
  }
}
