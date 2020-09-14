package org.folio.rest.impl;

import io.vertx.core.AsyncResult;
import io.vertx.core.Context;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.Map;
import org.folio.rest.jaxrs.model.TenantAttributes;
import javax.ws.rs.core.Response;
import org.folio.rest.tools.utils.TenantLoading;
import org.folio.coursereserves.util.Util;
import org.folio.rest.jaxrs.model.Parameter;



public class CoursesTenantAPI extends TenantAPI {

  public static final String SAMPLE_DATA_COURSELISTING = "c03bcba3-a6a0-4251-b316-0631bb2e6f21";
  public static final Logger logger = LoggerFactory.getLogger(CoursesTenantAPI.class);
  protected static final String PARAMETER_LOAD_SAMPLE = "loadSample";

  @Override
  public void postTenant(TenantAttributes tenantAttributes, Map<String, String> headers,
      Handler<AsyncResult<Response>> handler, Context context) {
    logger.info("Calling overridden postTenant method");
    Boolean loadSample;
    List<Parameter> parameterList = tenantAttributes.getParameters();
    Boolean loadSampleCandidate = null;
    for(Parameter parameter : parameterList) {      
      if(PARAMETER_LOAD_SAMPLE.equals(parameter.getKey())) {
        loadSampleCandidate = Boolean.parseBoolean(parameter.getValue());
      }      
    }
    loadSample = loadSampleCandidate != null ? loadSampleCandidate : false;
    super.postTenant(tenantAttributes, headers, res -> {
      if(res.failed()) {
        logger.error("Unable to load tenant: " + res.cause().getMessage());
        handler.handle(res);
      } else if(Boolean.TRUE.equals(loadSample)) {
        TenantLoading tenantLoading = new TenantLoading();
        tenantLoading
            .withKey(PARAMETER_LOAD_SAMPLE).withLead("sample-data").withPostOnly()
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
          }
          // We're gonna go ahead and return success even if the sample load fails
          handler.handle(res); //This should allow the proper response for upgrade or install
          
        });
            
      } else {
        handler.handle(res);
      }
    },
    context);
  }
  
}
