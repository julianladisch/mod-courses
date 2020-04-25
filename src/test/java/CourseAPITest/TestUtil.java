package CourseAPITest;

import io.vertx.core.Future;
import io.vertx.core.Promise;
import io.vertx.core.Vertx;
import io.vertx.core.http.CaseInsensitiveHeaders;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import java.util.Map;


public class TestUtil {
  private static HttpClient httpClient = null;

  static class WrappedResponse {
    private String explanation;
    private int code;
    private String body;
    private JsonObject json;
    private HttpClientResponse response;

    public WrappedResponse(String explanation, int code, String body,
        HttpClientResponse response) {
      this.explanation = explanation;
      this.code = code;
      this.body = body;
      this.response = response;
      try {
        json = new JsonObject(body);
      } catch(Exception e) {
        json = null;
      }
    }

    public String getExplanation() {
      return explanation;
    }

    public int getCode() {
      return code;
    }

    public String getBody() {
      return body;
    }

    public HttpClientResponse getResponse() {
      return response;
    }

    public JsonObject getJson() {
      return json;
    }

  }

  private static HttpClient getHttpClient(Vertx vertx) {
    if(httpClient == null) {
      httpClient = vertx.createHttpClient();
    }
    return httpClient;
  }

  public static Future<WrappedResponse> doOkapiRequest(Vertx vertx, String requestPath,
      HttpMethod method, Map<String, String> okapiHeaders, Map<String, String> extraHeaders,
      String payload, Integer expectedCode, String explanation) {
    HttpClient client = getHttpClient(vertx);
    CaseInsensitiveHeaders headers = new CaseInsensitiveHeaders();
    CaseInsensitiveHeaders originalHeaders = new CaseInsensitiveHeaders();
    originalHeaders.setAll(okapiHeaders);
    String okapiUrl = originalHeaders.get("x-okapi-url");
    if(okapiUrl == null) {
      return Future.failedFuture("No okapi URL found in headers");
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
    HttpClientRequest request = client.requestAbs(method, requestUrl);
    for(Map.Entry entry : headers.entries()) {
      String key = (String)entry.getKey();
      String value = (String)entry.getValue();
      if( key != null && value != null) {
        request.putHeader(key, value);
      }
    }
    return wrapRequestResponse(request, payload, expectedCode, explanation);
  }

  public static Future<WrappedResponse> doRequest(Vertx vertx, String url,
          HttpMethod method, CaseInsensitiveHeaders headers, String payload,
          Integer expectedCode, String explanation) {
    //Future<WrappedResponse> future = Future.future();
    boolean addPayLoad = false;
    //HttpClient client = vertx.createHttpClient();
    HttpClient client = getHttpClient(vertx);
    HttpClientRequest request = client.requestAbs(method, url);
    //Add standard headers
    request.putHeader("X-Okapi-Tenant", "diku")
            .putHeader("content-type", "application/json")
            .putHeader("accept", "application/json");
    if(headers != null) {
      for(Map.Entry entry : headers.entries()) {
        request.putHeader((String)entry.getKey(), (String)entry.getValue());
        System.out.println(String.format("Adding header '%s' with value '%s'",
            (String)entry.getKey(), (String)entry.getValue()));
      }
    }
    /*
    //standard exception handler
    request.exceptionHandler(e -> { future.fail(e); });
    request.handler( req -> {
      req.bodyHandler(buf -> {
        String explainString = "(no explanation)";
        if(explanation != null) { explainString = explanation; }
        if(expectedCode != null && expectedCode != req.statusCode()) {
          future.fail(method.toString() + " to " + url + " failed. Expected status code "
                  + expectedCode + ", got status code " + req.statusCode() + ": "
                  + buf.toString() + " | " + explainString);
        } else {
          System.out.println("Got status code " + req.statusCode() + " with payload of: " + buf.toString() + " | " + explainString);
          WrappedResponse wr = new WrappedResponse(explanation, req.statusCode(), buf.toString(), req);
          future.complete(wr);
        }
      });
    });
    System.out.println("Sending " + method.toString() + " request to url '"+
              url + " with payload: " + payload + "'\n");
    if(method == HttpMethod.PUT || method == HttpMethod.POST) {
      request.end(payload);
    } else {
      request.end();
    }
    return future;
    */
    return wrapRequestResponse(request, payload, expectedCode, explanation);
  }

  /* HttpClientRequest should have headers set before calling */
  private static Future<WrappedResponse> wrapRequestResponse(HttpClientRequest request,
      String payload, Integer expectedCode, String explanation) {
    Promise<WrappedResponse> promise = Promise.promise();
    request.exceptionHandler(e -> { promise.fail(e); });
    request.handler( req -> {
      req.bodyHandler(buf -> {
        String explainString = "(no explanation)";
        if(explanation != null) { explainString = explanation; }
        if(expectedCode != null && expectedCode != req.statusCode()) {
          promise.fail(request.method().toString() + " to " + request.absoluteURI()
                  + " failed. Expected status code "
                  + expectedCode + ", got status code " + req.statusCode() + ": "
                  + buf.toString() + " | " + explainString);
        } else {
          System.out.println("Got status code " + req.statusCode() + " with payload of: " + buf.toString() + " | " + explainString);
          WrappedResponse wr = new WrappedResponse(explanation, req.statusCode(), buf.toString(), req);
          promise.complete(wr);
        }
      });
    });
    System.out.println("Sending " + request.method().toString() + " request to url '"+
              request.absoluteURI() + " with payload: " + payload + "'\n");
    if(request.method() == HttpMethod.PUT || request.method() == HttpMethod.POST) {
      request.end(payload);
    } else {
      request.end();
    }
    return promise.future();

  }
}

