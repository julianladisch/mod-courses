package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import java.util.Map;
import org.folio.rest.jaxrs.model.TenantAttributes;
import javax.ws.rs.core.Response;


public class CoursesTenantAPI extends TenantAPI {
  @Override
  public void postTenant(TenantAttributes ta, Map<String, String> headers, 
      Handler<AsyncResult<Response>> hndlr, Context cntxt) {
    hndlr.handle(Future.succeededFuture(PostTenantResponse
      .respond201WithApplicationJson("")));
    //super.postTenant(ta, headers, hndlr, cntxt);
  }
  
}
