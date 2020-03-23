package org.folio.coursereserves.util;

import io.vertx.core.logging.Logger;


public class Util {
  public static String logAndSaveError(Throwable err, Logger logger) {
    String message = err.getLocalizedMessage();
    logger.error(message, err);
    return message;
  }
}
