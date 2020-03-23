package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.Map;
import org.folio.rest.jaxrs.model.TenantAttributes;
import javax.ws.rs.core.Response;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.coursereserves.util.Util;



public class CoursesTenantAPI extends TenantAPI {

  public static final String SAMPLE_DATA_COURSELISTING = "c03bcba3-a6a0-4251-b316-0631bb2e6f21";
  public static final Logger logger = LoggerFactory.getLogger(CoursesTenantAPI.class);

  @Override
  public void postTenant(TenantAttributes tenantAttributes, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context context) {
    logger.info("Calling overridden postTenant method");
    super.postTenant(tenantAttributes, headers, res -> {
      if(res.failed()) {
        String message = Util.logAndSaveError(res.cause(), logger);
        handler.handle(Future.succeededFuture(PostTenantResponse
            .respond500WithTextPlain(message)));
      } else {    
        TenantLoading tenantLoading = new TenantLoading();
        tenantLoading
            .withKey("loadSample").withLead("sample-data").withPostOnly()
              .add("terms", "coursereserves/terms")
              .add("copyrightstatuses", "coursereserves/copyrightstatuses")
              .add("departments", "coursereserves/departments")
              .add("processingstatuses", "coursereserves/processingstatuses")
              .add("coursetypes", "coursereserves/coursetypes")
              .add("courselistings", "coursereserves/courselistings")
              .add("courses", "coursereserves/courses")
              .add("instructors", "coursereserves/courselistings/"+SAMPLE_DATA_COURSELISTING+"/instructors")
              .add("reserves", "coursereserves/reserves");
        tenantLoading.perform(tenantAttributes, headers, context.owner(), 
            performRes -> {
          if(performRes.failed()) {
            String message = Util.logAndSaveError(performRes.cause(), logger);
            handler.handle(Future.succeededFuture(PostTenantResponse
                .respond500WithTextPlain("Error calling perform() " + message)));
          } else {
            handler.handle(Future.succeededFuture(PostTenantResponse
                .respond201WithApplicationJson("")));
          }
        });
            
      }
    },
    context);
  }
  
}
