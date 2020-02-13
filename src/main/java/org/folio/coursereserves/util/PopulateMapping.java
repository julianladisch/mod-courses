package org.folio.coursereserves.util;

public class PopulateMapping {
  public static enum ImportType { STRING, INTEGER, BOOLEAN, STRINGLIST };
  public final String methodName;
  public final String fieldName;
  public final ImportType type;
  
  public PopulateMapping(String fieldName, ImportType type, String methodName) {
    this.methodName = methodName;
    this.fieldName = fieldName;
    this.type = type;
  }
  
  public PopulateMapping(String fieldName, ImportType type) {
    this(fieldName, type, "set"+fieldName.substring(0,1).toUpperCase()+fieldName.substring(1));
  }
  
  public PopulateMapping(String fieldName) {
    this(fieldName, ImportType.STRING);
  }
  
}
