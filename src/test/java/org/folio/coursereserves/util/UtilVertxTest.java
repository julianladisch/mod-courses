package org.folio.coursereserves.util;

import io.vertx.core.Future;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.unit.Async;
import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.UUID;
import org.folio.rest.jaxrs.model.CopiedItem;
import org.folio.rest.jaxrs.model.CopyrightTracking;
import org.folio.rest.jaxrs.model.Copyrightstatus;
import org.folio.rest.jaxrs.model.Processingstatus;
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
      Processingstatus ps = new Processingstatus();
      ps.setId(UUID.randomUUID().toString());
      Future<Processingstatus> processingStatusFuture = Future.succeededFuture(ps);
      Copyrightstatus cs = new Copyrightstatus();
      cs.setId(UUID.randomUUID().toString());
      Future<Copyrightstatus> copyrightStatusFuture = Future.succeededFuture(cs);
      Future<JsonObject> loanTypeFuture =
          Future.succeededFuture(new JsonObject().put("id", UUID.randomUUID().toString()));
      CRUtil.populateReserve(reserve, tempLocationFuture, permLocationFuture,
          processingStatusFuture, copyrightStatusFuture, loanTypeFuture)
          .setHandler(res -> {
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
