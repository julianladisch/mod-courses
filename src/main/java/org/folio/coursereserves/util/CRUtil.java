package org.folio.coursereserves.util;

import io.vertx.core.CompositeFuture;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.folio.coursereserves.util.PopulateMapping.ImportType;
import static org.folio.rest.impl.CourseAPI.COPYRIGHT_STATUSES_TABLE;
import static org.folio.rest.impl.CourseAPI.COURSE_LISTINGS_TABLE;
import static org.folio.rest.impl.CourseAPI.COURSE_TYPES_TABLE;
import static org.folio.rest.impl.CourseAPI.DEPARTMENTS_TABLE;
import static org.folio.rest.impl.CourseAPI.INSTRUCTORS_TABLE;
import static org.folio.rest.impl.CourseAPI.PROCESSING_STATUSES_TABLE;
import static org.folio.rest.impl.CourseAPI.RESERVES_TABLE;
import static org.folio.rest.impl.CourseAPI.TERMS_TABLE;
import org.folio.rest.jaxrs.model.Contributor;
import org.folio.rest.jaxrs.model.CopiedItem;
import org.folio.rest.jaxrs.model.CopyrightStatusObject;
import org.folio.rest.jaxrs.model.CopyrightStatus;
import org.folio.rest.jaxrs.model.Course;
import org.folio.rest.jaxrs.model.CourseListing;
import org.folio.rest.jaxrs.model.CourseListingObject;
import org.folio.rest.jaxrs.model.CourseTypeObject;
import org.folio.rest.jaxrs.model.CourseType;
import org.folio.rest.jaxrs.model.Department;
import org.folio.rest.jaxrs.model.DepartmentObject;
import org.folio.rest.jaxrs.model.HoldShelfExpiryPeriod;
import org.folio.rest.jaxrs.model.HoldShelfExpiryPeriod.IntervalId;
import org.folio.rest.jaxrs.model.Instructor;
import org.folio.rest.jaxrs.model.InstructorObject;
import org.folio.rest.jaxrs.model.LocationObject;
import org.folio.rest.jaxrs.model.ProcessingStatusObject;
import org.folio.rest.jaxrs.model.ProcessingStatus;
import org.folio.rest.jaxrs.model.Publication;
import org.folio.rest.jaxrs.model.Reserve;
import org.folio.rest.jaxrs.model.ServicepointObject;
import org.folio.rest.jaxrs.model.StaffSlip;
import org.folio.rest.jaxrs.model.TemporaryLoanTypeObject;
import org.folio.rest.jaxrs.model.TemporaryLocationObject;
import org.folio.rest.jaxrs.model.Term;
import org.folio.rest.jaxrs.model.TermObject;
import org.folio.rest.persist.Criteria.Criteria;
import org.folio.rest.persist.Criteria.Criterion;
import org.folio.rest.persist.PgUtil;
import org.folio.rest.persist.PostgresClient;


public class CRUtil {

  public static final Logger logger = LoggerFactory.getLogger(
          CRUtil.class);

  public static final String SERVICE_POINTS_ENDPOINT = "/service-points";
  public static final String LOCATIONS_ENDPOINT = "/locations";
  public static final String LOAN_TYPES_ENDPOINT = "/loan-types";
  public static final String ITEMS_ENDPOINT = "/item-storage/items";
  public static final String HOLDINGS_ENDPOINT = "/holdings-storage/holdings";
  public static final String INSTANCES_ENDPOINT = "/instance-storage/instances";

  protected static final List<PopulateMapping> LOCATION_MAP_LIST = getLocationMapList();

  protected static final Map<String, String> textAcceptHeaders = getTextAcceptHeaders();

  public static PostgresClient getPgClient(Map<String, String> okapiHeaders,
      Context context) {
    return PgUtil.postgresClient(context, okapiHeaders);
  }

  public static List<PopulateMapping> getLocationMapList() {
    List<PopulateMapping> mapList = new ArrayList();
    mapList.add(new PopulateMapping("id"));
    mapList.add(new PopulateMapping("name"));
    mapList.add(new PopulateMapping("code"));
    mapList.add(new PopulateMapping("description"));
    mapList.add(new PopulateMapping("discoveryDisplayName"));
    mapList.add(new PopulateMapping("isActive", ImportType.BOOLEAN));
    mapList.add(new PopulateMapping("institutionId"));
    mapList.add(new PopulateMapping("campusId"));
    mapList.add(new PopulateMapping("libraryId"));
    mapList.add(new PopulateMapping("primaryServicePoint"));
    mapList.add(new PopulateMapping("servicePointIds", ImportType.STRINGLIST));

    return mapList;
  }

  public static Map<String, String> getTextAcceptHeaders() {
    Map<String, String> acceptMap = new HashMap<>();
    acceptMap.put("Accept", "text/plain");
    return acceptMap;
  }

  public static Future<JsonObject> populateReserveInventoryCache(Reserve reserve,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    Future<String> itemIdFuture;
    String barcode;
    if(reserve.getCopiedItem() != null) {
      barcode = reserve.getCopiedItem().getBarcode();
    } else {
      barcode = null;
    }
    String itemId = reserve.getItemId();
    if(itemId != null && barcode == null) {
      itemIdFuture = Future.succeededFuture(itemId);
    } else if(barcode != null) {
      itemIdFuture = Future.future();
      //String barcode = reserve.getCopiedItem().getBarcode();
      lookupItemByBarcode(barcode, okapiHeaders, context)
          .setHandler(barcodeItemLookupRes -> {
        if(barcodeItemLookupRes.failed()) {
          itemIdFuture.fail(barcodeItemLookupRes.cause());
        } else {
          if(barcodeItemLookupRes.result() != null) {
            itemIdFuture.complete(barcodeItemLookupRes.result().getString("id"));
          } else {
            itemIdFuture.fail("No item found for barcode " + barcode);
          }
        }
      });
    } else {
      future.fail("Must provide item id or item barcode to populate copied items");
      return future;
    }
    itemIdFuture.setHandler(itemIdRes -> {
      if(itemIdRes.failed()) {
        logger.error("Failed to get item id " + itemIdRes.cause().getLocalizedMessage());
        future.fail(itemIdRes.cause());
      } else {
        reserve.setItemId(itemIdRes.result());
        String retrievedItemId = itemIdRes.result();
        logger.info("Looking up information for item " + retrievedItemId
            + " from inventory module");
        lookupItemHoldingsInstanceByItemId(retrievedItemId, okapiHeaders, context)
            .setHandler(inventoryRes -> {
          if(inventoryRes.failed()) {
            logger.error("Unable to do inventory lookup: "
                + inventoryRes.cause().getLocalizedMessage());
            future.fail(inventoryRes.cause());
          } else {
            try {
              logger.info("Attempting to populate copied items with inventory lookup for item id "
                  + retrievedItemId);
              populateReserveCopiedItemFromJson(reserve, inventoryRes.result());
              future.complete(inventoryRes.result());
            } catch(Exception e) {
              future.fail(e);
            }
          }
        });
      }
    });
    return future;
  }

  public static Future<List<Reserve>> expandListOfReserves(List<Reserve> listOfReserves,
      Map<String, String> okapiHeaders, Context context) {
    Future<List<Reserve>> future = Future.future();
    List<Future> expandedReserveFutureList = new ArrayList<>();
    for(Reserve reserve : listOfReserves) {
      expandedReserveFutureList.add(lookupExpandedReserve(reserve.getId(),
          okapiHeaders, context, true));
    }
    CompositeFuture compositeFuture = CompositeFuture.all(expandedReserveFutureList);
    compositeFuture.setHandler(expandReservesRes -> {
      if(expandReservesRes.failed()) {
        future.fail(expandReservesRes.cause());
      } else {
        List<Reserve> newListOfReserves = new ArrayList<>();
        for( Future reserveFuture : expandedReserveFutureList ) {
          Future<Reserve> f = (Future<Reserve>)reserveFuture;
          newListOfReserves.add(f.result());
        }
        future.complete(newListOfReserves);
      }
    });
    return future;
  }

  public static String getStringValueFromObjectArray(String fieldName, JsonArray array) {
    try {
      //Current behavior is to only check the first element of the array. This may change.
      JsonObject eaJson = array.getJsonObject(0);
      return eaJson.getString(fieldName);
    } catch(Exception e) {
      return null;
    }
  }

  public static void populateReserveCopiedItemFromJson(Reserve reserve, JsonObject json) {
    JsonObject itemJson = json.getJsonObject("item");
    JsonObject instanceJson = json.getJsonObject("instance");
    JsonObject holdingsJson = json.getJsonObject("holdings");
    CopiedItem copiedItem = new CopiedItem();
    copiedItem.setBarcode(itemJson.getString("barcode"));
    copiedItem.setVolume(itemJson.getString("volume"));
    copiedItem.setTitle(instanceJson.getString("title"));
    copiedItem.setEnumeration(itemJson.getString("enumeration"));
    copiedItem.setInstanceId(instanceJson.getString("id"));
    copiedItem.setHoldingsId(holdingsJson.getString("id"));
    try {
      if (itemJson.containsKey("copyNumber")) {
        copiedItem.setCopy(itemJson.getString("copyNumber"));
      } else if (itemJson.containsKey("copyNumbers")) {
        copiedItem.setCopy(itemJson.getJsonArray("copyNumbers").getString(0));
      }
    } catch (Exception e) {
      logger.info("Unable to copy copyNumber(s) field from item: " + e.getLocalizedMessage());
    }
    try {
      JsonArray eaItemJsonArray = itemJson.getJsonArray("electronicAccess");
      JsonArray eaHoldingsJsonArray = holdingsJson.getJsonArray("electronicAccess");
      String uri = getStringValueFromObjectArray("uri", eaItemJsonArray);
      String publicNote = getStringValueFromObjectArray("publicNote", eaItemJsonArray);
      if(uri == null) {
        uri = getStringValueFromObjectArray("uri", eaHoldingsJsonArray);
      }
      if(publicNote == null) {
        publicNote = getStringValueFromObjectArray("publicNote", eaHoldingsJsonArray);
      }
      copiedItem.setUri(uri);
      copiedItem.setUrl(publicNote);
    } catch(Exception e) {
      logger.info("Unable to copy electronic access field from item: " + e.getLocalizedMessage());
    }
    String permanentLocationId = itemJson.getString("permanentLocationId");
    if(permanentLocationId == null) {
      permanentLocationId = holdingsJson.getString("permanentLocationId");
    }
    copiedItem.setPermanentLocationId(permanentLocationId);
    copiedItem.setTemporaryLocationId(itemJson.getString("temporaryLocationId"));
    String callNumber = makeCallNumber(itemJson.getString("itemLevelCallNumberPrefix"),
        itemJson.getString("itemLevelCallNumber"), itemJson.getString("itemLevelCallNumberSuffix"));
    if(callNumber == null) {
      callNumber = makeCallNumber(holdingsJson.getString("callNumberPrefix"),
          holdingsJson.getString("callNumber"), holdingsJson.getString("callNumberSuffix"));
    }
    copiedItem.setCallNumber(callNumber);
    String temporaryLoanTypeId = itemJson.getString("temporaryLoanTypeId");
    if(reserve.getTemporaryLoanTypeId() == null) {
      reserve.setTemporaryLoanTypeId(temporaryLoanTypeId);
    }
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

  public static Future<JsonObject> lookupItemHoldingsInstanceByItemId(String itemId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    JsonObject result = new JsonObject();
    logger.info("Making request for item at " + ITEMS_ENDPOINT + "/" + itemId);
    makeOkapiRequest(context.owner(), okapiHeaders, ITEMS_ENDPOINT + "/" + itemId,
        HttpMethod.GET, null, null, 200).setHandler(itemRes -> {
      if(itemRes.failed()) {
        logger.error("Unable to lookup item by id " + itemId);
        future.fail(itemRes.cause());
      } else {
        JsonObject itemJson = itemRes.result();
        String holdingsId = itemJson.getString("holdingsRecordId");
        result.put("item", itemJson);
        logger.info("Making request for holdings at " + HOLDINGS_ENDPOINT + "/" + holdingsId);
        makeOkapiRequest(context.owner(), okapiHeaders, HOLDINGS_ENDPOINT + "/" + holdingsId,
            HttpMethod.GET, null, null, 200).setHandler(holdingsRes -> {
          if(holdingsRes.failed()) {
            future.fail(holdingsRes.cause());
          } else {
            JsonObject holdingsJson = holdingsRes.result();
            String instanceId = holdingsJson.getString("instanceId");
            result.put("holdings", holdingsJson);
            logger.info("Making request for instance at " + INSTANCES_ENDPOINT + "/" + instanceId);
            makeOkapiRequest(context.owner(), okapiHeaders, INSTANCES_ENDPOINT
                + "/" + instanceId, HttpMethod.GET, null, null, 200).setHandler(
                instanceRes -> {
              if(instanceRes.failed()) {
                future.fail(instanceRes.cause());
              } else {
                JsonObject instanceJson = instanceRes.result();
                result.put("instance", instanceJson);
                logger.info("Inventory lookup complete");
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
    if(okapiUrl == null) {
      future.fail("No okapi URL found in headers");
      return future;
    }
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
    logger.debug("Creating request for url " + requestUrl);
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
            String message = String.format(
                "Expected status code %s for %s request to url %s, got %s: %s",
                expectedCode, method.toString(), requestUrl, requestRes.statusCode(),
                response);
            logger.error(message);
            future.fail(message);
          } else {
            if(response != null && response.length() > 0) {
              future.complete(new JsonObject(response));
            } else {
              future.complete(null);
            }
          }
        } catch(Exception e) {
          future.fail(e);
        }
      });
    });
    if(method == HttpMethod.PUT || method == HttpMethod.POST) {
      request.end(payload);
      logger.debug("Sending request with payload");
    } else {
      logger.debug("Sending payload-free request");
      request.end();
    }
    return future;
  }

  public static Future<Reserve> lookupExpandedReserve(String reserveId,
      Map<String, String> okapiHeaders, Context context, Boolean expand) {
    Future<Reserve> future = Future.future();
    getReserveById(reserveId, okapiHeaders, context).setHandler(reserveRes -> {
      if(reserveRes.failed()) {
        future.fail(reserveRes.cause());
      } else if(expand == false ||  reserveRes.result() == null ||
          reserveRes.result().getCopiedItem() == null) {
        future.complete(reserveRes.result());
      } else {
        try {
          Reserve reserve = reserveRes.result();
          Future<JsonObject> tempLocationFuture = lookupLocation(
              reserve.getCopiedItem().getTemporaryLocationId(), okapiHeaders, context);
          Future<JsonObject> permLocationFuture = lookupLocation(
              reserve.getCopiedItem().getPermanentLocationId(), okapiHeaders, context);
          Future<ProcessingStatus> processingStatusFuture;
          if(reserve.getProcessingStatusId() != null) {
            processingStatusFuture = lookupProcessingStatus(
              reserve.getProcessingStatusId(), okapiHeaders, context);
          } else {
            processingStatusFuture = Future.failedFuture("No processing status id");
          }
          Future<CopyrightStatus> copyrightStatusFuture;
          if(reserve.getCopyrightTracking() != null
              && reserve.getCopyrightTracking().getCopyrightStatusId() != null) {
            copyrightStatusFuture = lookupCopyrightStatus(
            reserve.getCopyrightTracking().getCopyrightStatusId(), okapiHeaders,
                context);
          } else {
            copyrightStatusFuture = Future.failedFuture("No copyright tracking object");
          }
          Future<JsonObject> loanTypeFuture;
          if(reserve.getTemporaryLoanTypeId() != null) {
            loanTypeFuture = lookupLoanType(reserve.getTemporaryLoanTypeId(),
                okapiHeaders, context);
          } else {
            loanTypeFuture = Future.failedFuture("No temporary loan type id");
          }
          populateReserveForRetrieval(reserve, tempLocationFuture, permLocationFuture,
              processingStatusFuture, copyrightStatusFuture, loanTypeFuture)
              .setHandler(populateRes -> {
            if(populateRes.failed()) {
              future.fail(populateRes.cause());
            } else {
              future.complete(reserve);
            }
          });
        } catch(Exception e) {
          future.fail(e);
        }
      }
    });
    return future;
  }

  public static Future<JsonObject> lookupItemByBarcode(String barcode,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    String itemRequestUrl = String.format("%s?query=barcode=%s", ITEMS_ENDPOINT,
        barcode);
    logger.debug("Looking up item by barcode with url " + itemRequestUrl);
    makeOkapiRequest(context.owner(), okapiHeaders, itemRequestUrl, HttpMethod.GET,
        null, null, 200).setHandler(itemQueryRes -> {
      if(itemQueryRes.failed()) {
        future.fail(itemQueryRes.cause());
      } else {
        try {
          JsonObject itemsResultJson = itemQueryRes.result();
          if(itemsResultJson.getInteger("totalRecords") > 1) {
            future.fail(String.format("Expected 1 result for barcode %s, got multiple",
                barcode));
          } else if(itemsResultJson.getInteger("totalRecords") < 1) {
            future.complete(null);
          } else {
            JsonObject itemJson = itemsResultJson.getJsonArray("items").getJsonObject(0);
            future.complete(itemJson);
          }
        } catch(Exception e) {
          future.fail(e);
        }
      }
    });
    return future;
  }

  public static Future<Void> populateReserveForRetrieval(Reserve reserve,
      Future<JsonObject> tempLocationFuture, Future<JsonObject> permLocationFuture,
      Future<ProcessingStatus> processingStatusFuture,
      Future<CopyrightStatus> copyrightStatusFuture, Future<JsonObject> loanTypeFuture) {
    Future<Void> future = Future.future();
    List<Future> futureList = new ArrayList<>();
    futureList.add(tempLocationFuture);
    futureList.add(permLocationFuture);
    futureList.add(processingStatusFuture);
    futureList.add(copyrightStatusFuture);
    futureList.add(loanTypeFuture);
    CompositeFuture compositeFuture = CompositeFuture.join(futureList);
    compositeFuture.setHandler(compRes -> {
      try {
        if(reserve.getCopiedItem() != null) {
          if(tempLocationFuture.succeeded()) {
            reserve.getCopiedItem().setTemporaryLocationObject(
                temporaryLocationObjectFromJson(tempLocationFuture.result()));
          } else {
            logger.info("TemporaryLocationObject lookup failed "
                + tempLocationFuture.cause().getLocalizedMessage());
          }
          if(permLocationFuture.succeeded()) {
            reserve.getCopiedItem().setPermanentLocationObject(
                temporaryLocationObjectFromJson(permLocationFuture.result()));
          } else {
            logger.info("PermanentLocationObject lookup failed "
                + permLocationFuture.cause().getLocalizedMessage());
          }
        } else {
          logger.info("No copied item field in reserve to populate");
        }
        if(processingStatusFuture.succeeded()) {
          ProcessingStatusObject pso = new ProcessingStatusObject();
          copyFields(pso, processingStatusFuture.result());
          reserve.setProcessingStatusObject(pso);
        }
        if(copyrightStatusFuture.succeeded()) {
          CopyrightStatusObject cso = new CopyrightStatusObject();
          copyFields(cso, copyrightStatusFuture.result());
          reserve.getCopyrightTracking().setCopyrightStatusObject(cso);
        }
        if(loanTypeFuture.succeeded()) {
          TemporaryLoanTypeObject tlto = temporaryLoanTypeObjectFromJson(
              loanTypeFuture.result());
          reserve.setTemporaryLoanTypeObject(tlto);
        }
        future.complete();
      } catch(Exception e) {
        future.fail(e);
      }
    });
    return future;
  }

  public static Future<CourseListing> lookupExpandedCourseListing(String courseListingId,
      Map<String, String> okapiHeaders, Context context, Boolean expandTerm) {
    Future<CourseListing> future = Future.future();
    getCourseListingById(courseListingId, okapiHeaders, context).setHandler(clRes -> {
      if(clRes.failed()) {
        future.fail(clRes.cause());
      } else if(clRes.result() == null) {
        future.complete(null);
      } else {
        try {
          CourseListing courselisting = clRes.result();
          String termId = courselisting.getTermId();
          String courseTypeId = courselisting.getCourseTypeId();
          String locationId = courselisting.getLocationId();
          String servicepointId = courselisting.getServicepointId();
          Future<Term> termFuture;
          Future<CourseType> coursetypeFuture;
          Future<JsonObject> locationFuture;
          Future<JsonObject> servicePointFuture;
          if(expandTerm && termId != null) {
            termFuture = lookupTerm(termId, okapiHeaders, context);
          } else {
            termFuture = Future.failedFuture("No lookup");
          }
          if(expandTerm && courseTypeId != null) {
            coursetypeFuture = lookupCourseType(courseTypeId, okapiHeaders, context);
          } else {
            coursetypeFuture = Future.failedFuture("No lookup");
          }
          if(expandTerm && locationId != null) {
            locationFuture = lookupLocation(locationId, okapiHeaders, context);
          } else {
            locationFuture = Future.failedFuture("No lookup");
          }
          if(expandTerm && servicepointId != null) {
            servicePointFuture = lookupServicepoint(servicepointId, okapiHeaders, context);
          } else {
            servicePointFuture = Future.failedFuture("No lookup");
          }

          List<Future> futureList = new ArrayList<>();
          futureList.add(termFuture);
          futureList.add(coursetypeFuture);
          futureList.add(locationFuture);
          futureList.add(servicePointFuture);
          CompositeFuture compositeFuture = CompositeFuture.join(futureList);
          compositeFuture.setHandler(compRes -> {
            try {
              if(termFuture.succeeded()) {
                courselisting.setTermObject(termObjectFromTerm(termFuture.result()));
              }
              if(coursetypeFuture.succeeded()) {
                courselisting.setCourseTypeObject(courseTypeObjectFromCourseType(
                    coursetypeFuture.result()));
              }
              if(locationFuture.succeeded()) {
                courselisting.setLocationObject(locationObjectFromJson(locationFuture.result()));
              }
              if(servicePointFuture.succeeded()) {
                courselisting.setServicepointObject(servicepointObjectFromJson(servicePointFuture.result()));
              }
              future.complete(courselisting);
            } catch(Exception e) {
              future.fail(e);
            }
          });
        } catch(Exception e) {
          future.fail(e);
        }
      }
    });
    return future;
  }

  /* Basic lookup for courselisting, wrapped in a future */
  public static Future<CourseListing> getCourseListingById(String courseListingId,
      Map<String, String> okapiHeaders, Context context) {
    Future<CourseListing> future = Future.future();
    logger.info("Looking up course listing for id '" + courseListingId + "'");
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
    postgresClient.getById(COURSE_LISTINGS_TABLE, courseListingId, CourseListing.class,
        courseListingReply -> {
      if(courseListingReply.failed()) {
        future.fail(courseListingReply.cause());
      } else if(courseListingReply.result() == null) {
        future.complete(null);
      } else {
        future.complete(courseListingReply.result());
      }
    });
    return future;
  }

  public static Future<Reserve> getReserveById(String reserveId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Reserve> future = Future.future();
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
    postgresClient.getById(RESERVES_TABLE, reserveId, Reserve.class,
        reserveReply -> {
      if(reserveReply.failed()) {
        future.fail(reserveReply.cause());
      } else {
        future.complete(reserveReply.result());
      }
    });
    return future;
  }

  public static void populatePojoFromJson(Object pojo, JsonObject json,
      List<PopulateMapping> mapList) throws NoSuchMethodException,
      IllegalAccessException, IllegalArgumentException, InvocationTargetException {
    for( PopulateMapping popMap : mapList ) {
      Object value = null;
      Method method = null;
      if(popMap.type.equals(ImportType.STRING)) {
        value = json.getString(popMap.fieldName);
        method = pojo.getClass().getMethod(popMap.methodName, String.class);
      } else if(popMap.type.equals(ImportType.INTEGER)) {
        value = json.getInteger(popMap.fieldName);
        method = pojo.getClass().getMethod(popMap.methodName, Integer.class);
      } else if(popMap.type.equals(ImportType.BOOLEAN)) {
        value = json.getBoolean(popMap.fieldName);
        method = pojo.getClass().getMethod(popMap.methodName, Boolean.class);
      } else if(popMap.type.equals(ImportType.STRINGLIST)) {
        List<String> stringList = new ArrayList<>();
        JsonArray jsonArray = json.getJsonArray(popMap.fieldName);
        if(jsonArray != null) {
          for(Object ob : jsonArray) {
            stringList.add((String) ob);
          }
        }
        method = pojo.getClass().getMethod(popMap.methodName, List.class);
        value = stringList;
      } else {
        throw new RuntimeException(popMap.type + " is not a valid type");
      }
      if(value != null) {
        method.invoke(pojo, value);
      }
    }
  }

  public static void copyFields(Object destinationPojo, Object sourcePojo) {
    if(destinationPojo == null || sourcePojo == null) {
      return;
    }
    Method[] destinationMethods = destinationPojo.getClass().getMethods();
    String patternString = "set(.+)";
    Pattern pattern = Pattern.compile(patternString);
    for(Method method : destinationMethods) {
      String name = method.getName();
      Matcher matcher = pattern.matcher(name);
      if(!matcher.find()) {
        continue;
      }
      String methodPart = matcher.group(1);
      String getName = "get" + methodPart;
      try {
        Method getMethod = sourcePojo.getClass().getMethod(getName);
        method.invoke(destinationPojo, getMethod.invoke(sourcePojo));
      } catch(Exception e) {
        logger.error(e.getLocalizedMessage());
      }
    }
  }

  public static Future<JsonObject> lookupLocation(String locationId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    String locationPath = LOCATIONS_ENDPOINT + "/" + locationId;
    logger.debug("Making request for location at " + locationPath);
    makeOkapiRequest(context.owner(), okapiHeaders, locationPath, HttpMethod.GET,
        null, null, 200).setHandler(locationRes-> {
      if(locationRes.failed()) {
        logger.error("Location request failed: " + locationRes.cause().getLocalizedMessage());
        future.fail(locationRes.cause());
      } else {
        logger.debug("Location request succeeded");
        JsonObject locationJson = locationRes.result();
        future.complete(locationJson);
      }
    });
    return future;
  }

  public static Future<JsonObject> lookupLoanType(String loanTypeId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    String loanTypePath = LOAN_TYPES_ENDPOINT + "/" + loanTypeId;
    logger.debug("Making request for location at " + loanTypePath);
    makeOkapiRequest(context.owner(), okapiHeaders, loanTypePath, HttpMethod.GET,
        null, null, 200).setHandler(loanTypeRes-> {
      if(loanTypeRes.failed()) {
        logger.error("Loan type request failed");
        future.fail(loanTypeRes.cause());
      } else {
        logger.debug("Loan type request succeeded");
        JsonObject loanTypeJson = loanTypeRes.result();
        future.complete(loanTypeJson);
      }
    });
    return future;
  }

  public static Future<JsonObject> lookupServicepoint(String servicepointId,
      Map<String, String> okapiHeaders, Context context) {
    Future<JsonObject> future = Future.future();
    String servicePointPath = SERVICE_POINTS_ENDPOINT + "/" + servicepointId;
    logger.debug("Making request for servicepoint at " + servicePointPath);
    makeOkapiRequest(context.owner(), okapiHeaders, servicePointPath,
        HttpMethod.GET, null, null, 200).setHandler(spRes -> {
      if(spRes.failed()) {
        future.fail(spRes.cause());
      } else {
        JsonObject spJson = spRes.result();
        future.complete(spJson);
      }
    });
    return future;
  }

  public static Future<List<Instructor>> lookupInstructorsForCourseListing(
      String courseListingId, Map<String, String> okapiHeaders, Context context) {
    Future<List<Instructor>> future = Future.future();
    try {
      PostgresClient postgresClient = getPgClient(okapiHeaders, context);
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
    } catch(Exception e) {
      future.fail(e);
    }
    return future;
  }

  public static Future<Void> updateCourseListingInstructorCache(String courseListingId,
      Map<String, String> okapiHeaders, Context context) {
    Promise<Void> promise = Promise.promise();
    lookupInstructorsForCourseListing(courseListingId,
        okapiHeaders, context).setHandler(instructorRes -> {
      if(instructorRes.failed()) {
        promise.fail(instructorRes.cause());
      } else {
        getCourseListingById(courseListingId, okapiHeaders, context)
            .setHandler(getRes -> {
          if(getRes.failed()) {
            promise.fail(getRes.cause());
          } else {
            try {
              CourseListing courseListing = getRes.result();
              List<Instructor> instructorList = instructorRes.result();
              logger.info("Found " + instructorList.size() + " instructors for listing " + courseListingId);
              courseListing.setInstructorObjects(instructorObjectListFromInstructorList(
                  instructorList));
              PostgresClient postgresClient = getPgClient(okapiHeaders, context);
              postgresClient.update(COURSE_LISTINGS_TABLE, courseListing, courseListingId,
                  putReply -> {
                if(putReply.failed()) {
                  promise.fail(putReply.cause());
                } else {
                  promise.complete();
                }
              });
            } catch(Exception e) {
              promise.fail(e);
            }
          }
        });
      }
    });
    return promise.future();
  }

  public static Future<Term> lookupTerm(String termId,
      Map<String, String> okapiHeaders, Context context) {
    Future<Term> future = Future.future();
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
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
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
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

  public static Future<CourseType> lookupCourseType(String courseTypeId,
      Map<String, String> okapiHeaders, Context context) {
    Future<CourseType> future = Future.future();
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
    postgresClient.getById(COURSE_TYPES_TABLE, courseTypeId, CourseType.class,
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

  public static Future<ProcessingStatus> lookupProcessingStatus(String processingStatusId,
      Map<String, String> okapiHeaders, Context context) {
    Future<ProcessingStatus> future = Future.future();
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
    postgresClient.getById(PROCESSING_STATUSES_TABLE, processingStatusId,
        ProcessingStatus.class,
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

  public static Future<CopyrightStatus> lookupCopyrightStatus(String copyrightStatusId,
      Map<String, String> okapiHeaders, Context context) {
    Future<CopyrightStatus> future = Future.future();
    PostgresClient postgresClient = getPgClient(okapiHeaders, context);
    postgresClient.getById(COPYRIGHT_STATUSES_TABLE, copyrightStatusId,
        CopyrightStatus.class,
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
    Future<CourseListing> courseListingFuture;
    Course newCourse;
    try {
      PostgresClient postgresClient = getPgClient(okapiHeaders, context);
      newCourse = copyCourse(course);
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
          try {
          CourseListingObject expandedCourseListing = new CourseListingObject();
          CourseListing courseListing = courselistingReply.result();
          if(courseListing != null) {
            copyFields(expandedCourseListing, courseListing);
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
              try {
                if(department != null) {
                  DepartmentObject departmentObject = new DepartmentObject();
                  copyFields(departmentObject, department);
                  newCourse.setDepartmentObject(departmentObject);
                }
              future.complete(newCourse);
              } catch(Exception e) {
                future.fail(e);
              }
            }
          });
          } catch(Exception e) {
            future.fail(e);
          }
        }
      });
    }
      catch(Exception e) {
        future.fail(e);
    }
    return future;
  }

  public static Future<Void> putItemUpdate(JsonObject itemJson,
      Map<String, String> okapiHeaders, Context context) {
    Future future = Future.future();
    try {
       String id = itemJson.getString("id");
       String putPath = ITEMS_ENDPOINT + "/" + id;
       makeOkapiRequest(context.owner(), okapiHeaders, putPath, HttpMethod.PUT,
           textAcceptHeaders, itemJson.encode(), 204).setHandler(res -> {
         if(res.failed()) {
           logger.error("Put failed: " + res.cause().getLocalizedMessage());
           future.fail(res.cause());
         } else {
           future.complete();
         }
       });
    } catch(Exception e) {
      future.fail(e);
    }
    return future;
  }

  private static Course copyCourse(Course originalCourse) {
    Course newCourse = new Course();
    copyFields(newCourse, originalCourse);
    return newCourse;
  }

  private static TermObject termObjectFromTerm(Term term) {
    TermObject termObject = new TermObject();
    termObject.setEndDate(term.getEndDate());
    termObject.setStartDate(term.getStartDate());
    termObject.setId(term.getId());
    termObject.setName(term.getName());
    return termObject;
  }

  private static CourseTypeObject courseTypeObjectFromCourseType(CourseType coursetype) {
    CourseTypeObject courseTypeObject = new CourseTypeObject();
    courseTypeObject.setId(coursetype.getId());
    courseTypeObject.setDescription(coursetype.getDescription());
    courseTypeObject.setName(coursetype.getName());
    return courseTypeObject;
  }

  private static LocationObject locationObjectFromJson(JsonObject json) {
    if(json == null) {
      return null;
    }
    LocationObject locationObject = new LocationObject();
    try {
      populatePojoFromJson(locationObject, json, LOCATION_MAP_LIST);
    } catch(Exception e) {
      logger.error("Unable to create location object from json: " + e.getLocalizedMessage());
      return null;
    }
    return locationObject;
  }


  /*
     Highly annoying to have to pretty much clone this function because of the odd
     POJO name generation that RMB uses. Is there a better way?
  */
  private static TemporaryLocationObject temporaryLocationObjectFromJson(JsonObject json) {
    TemporaryLocationObject locationObject = new TemporaryLocationObject();
    try {
      populatePojoFromJson(locationObject, json, LOCATION_MAP_LIST);
    } catch(Exception e) {
      logger.error("Unable to create temporary location object from json: "
          + e.getLocalizedMessage());
      return null;
    }
    return locationObject;
  }

  private static TemporaryLoanTypeObject temporaryLoanTypeObjectFromJson(JsonObject json) {
    TemporaryLoanTypeObject tlto = new TemporaryLoanTypeObject();
    tlto.setId(json.getString("id"));
    tlto.setName(json.getString("name"));
    return tlto;
  }

  private static ServicepointObject servicepointObjectFromJson(JsonObject json) {
    if(json == null) {
      return null;
    }
    ServicepointObject servicepointObject = new ServicepointObject();
    List<PopulateMapping> mapList = new ArrayList<>();
    mapList.add(new PopulateMapping("id"));
    mapList.add(new PopulateMapping("name"));
    mapList.add(new PopulateMapping("code"));
    mapList.add(new PopulateMapping("discoveryDisplayName"));
    mapList.add(new PopulateMapping("description"));
    mapList.add(new PopulateMapping("shelvingLagTime", ImportType.INTEGER));
    mapList.add(new PopulateMapping("pickupLocation", ImportType.BOOLEAN));
    try {
      populatePojoFromJson(servicepointObject, json, mapList);
    } catch(Exception e) {
      logger.error("Unable to create service point object from json: "
        +e.getLocalizedMessage());
      return null;
    }
    try {
      List<StaffSlip> staffSlipList = new ArrayList<>();
      JsonArray staffSlips = json.getJsonArray("staffSlips");
      for(int i = 0; i < staffSlips.size();i++) {
        JsonObject slip = staffSlips.getJsonObject(i);
        StaffSlip staffSlip = new StaffSlip();
        staffSlip.setId(slip.getString("id"));
        staffSlip.setPrintByDefault(slip.getBoolean("printByDefault"));
        staffSlipList.add(staffSlip);
      }
      if(!staffSlipList.isEmpty()) {
        servicepointObject.setStaffSlips(staffSlipList);
      }
    } catch(Exception e) {
      logger.error("Unable to add staffslips from json: " + e.getLocalizedMessage());
    }

    try {
      JsonObject hsepJson = json.getJsonObject("holdShelfExpiryPeriod");
      if(hsepJson != null) {
        HoldShelfExpiryPeriod hsep = new HoldShelfExpiryPeriod();
        hsep.setDuration(hsepJson.getInteger("duration"));
        String intervalIdString = hsepJson.getString("intervalId");
        if(intervalIdString.equals("Minutes")) {
          hsep.setIntervalId(IntervalId.MINUTES);
        } else if(intervalIdString.equals("Hours")) {
          hsep.setIntervalId(IntervalId.HOURS);
        } else if(intervalIdString.equals("Days")) {
          hsep.setIntervalId(IntervalId.DAYS);
        } else if(intervalIdString.equals("Weeks")) {
          hsep.setIntervalId(IntervalId.WEEKS);
        } else if(intervalIdString.equals("Months")) {
          hsep.setIntervalId(IntervalId.MONTHS);
        }
        servicepointObject.setHoldShelfExpiryPeriod(hsep);
      }
    } catch(Exception e) {
      logger.error("Unable to add hold shelf expiry from json: "
          + e.getLocalizedMessage());
    }
    return servicepointObject;
  }

  public static List<InstructorObject> instructorObjectListFromInstructorList(
      List<Instructor> instructorList) {
    List<InstructorObject> instructorObjectList = new ArrayList<>();
    for(Instructor instructor : instructorList) {
      InstructorObject instructorObject = new InstructorObject();
      copyFields(instructorObject, instructor);
      instructorObjectList.add(instructorObject);
    }
    return instructorObjectList;
  }

  public static String makeCallNumber(String prefix, String number, String suffix) {
    if(number == null || number.isEmpty()) {
      return null;
    }
    if(prefix == null) {
      prefix = "";
    }

    if(suffix == null) {
      suffix = "";
    }
    return prefix + number + suffix;
  }
}


