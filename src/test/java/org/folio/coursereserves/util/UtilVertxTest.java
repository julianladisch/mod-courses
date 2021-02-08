package org.folio.coursereserves.util;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.folio.rest.jaxrs.model.CopiedItem;
import org.folio.rest.jaxrs.model.CopyrightTracking;
import org.folio.rest.jaxrs.model.CopyrightStatus;
import org.folio.rest.jaxrs.model.ProcessingStatus;
import org.folio.rest.jaxrs.model.Reserve;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class UtilVertxTest {

  @Test
  public void TestPopulateReserve(TestContext context) {
    Async async = context.async();
    try {
      Reserve reserve = new Reserve();
      CopyrightTracking ct = new CopyrightTracking();
      reserve.setCopyrightTracking(ct);
      CopiedItem ci = new CopiedItem();
      reserve.setCopiedItem(ci);
      reserve.setId(UUID.randomUUID().toString());
      Future<JsonObject> tempLocationFuture =
          Future.succeededFuture(new JsonObject().put("id", UUID.randomUUID().toString()));
      Future<JsonObject> permLocationFuture =
          Future.succeededFuture(new JsonObject().put("id", UUID.randomUUID().toString()));
      ProcessingStatus ps = new ProcessingStatus();
      ps.setId(UUID.randomUUID().toString());
      Future<ProcessingStatus> processingStatusFuture = Future.succeededFuture(ps);
      CopyrightStatus cs = new CopyrightStatus();
      cs.setId(UUID.randomUUID().toString());
      Future<CopyrightStatus> copyrightStatusFuture = Future.succeededFuture(cs);
      Future<JsonObject> loanTypeFuture =
          Future.succeededFuture(new JsonObject().put("id", UUID.randomUUID().toString()));
      CRUtil.populateReserveForRetrieval(reserve, tempLocationFuture, permLocationFuture,
          processingStatusFuture, copyrightStatusFuture, loanTypeFuture)
          .onComplete(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
    }
  }

  @Test
  public void TestPopulateReserveWithFailedFutures(TestContext context) {
    Async async = context.async();
    try {
      Reserve reserve = new Reserve();
      CopyrightTracking ct = new CopyrightTracking();
      reserve.setCopyrightTracking(ct);
      CopiedItem ci = new CopiedItem();
      reserve.setCopiedItem(ci);
      reserve.setId(UUID.randomUUID().toString());
      Future<JsonObject> tempLocationFuture =
          Future.failedFuture("Failed location");
      Future<JsonObject> permLocationFuture =
          Future.failedFuture("Failed location");
      ProcessingStatus ps = new ProcessingStatus();
      ps.setId(UUID.randomUUID().toString());
      Future<ProcessingStatus> processingStatusFuture = Future.failedFuture("Failed status");
      CopyrightStatus cs = new CopyrightStatus();
      cs.setId(UUID.randomUUID().toString());
      Future<CopyrightStatus> copyrightStatusFuture = Future.failedFuture("Failed status");
      Future<JsonObject> loanTypeFuture =
          Future.failedFuture("Failed loantype");
      CRUtil.populateReserveForRetrieval(reserve, tempLocationFuture, permLocationFuture,
          processingStatusFuture, copyrightStatusFuture, loanTypeFuture)
          .onComplete(res -> {
        if(res.failed()) {
          context.fail(res.cause());
        } else {
          async.complete();
        }
      });
    } catch(Exception e) {
      e.printStackTrace();
      context.fail(e);
    }
  }

}
