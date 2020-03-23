package org.folio.coursereserves.util;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import java.util.List;
import java.util.UUID;
import java.util.ArrayList;
import org.folio.rest.impl.CourseAPI;
import org.folio.rest.jaxrs.model.LocationObject;
import org.folio.rest.jaxrs.model.Reserf;
import org.folio.rest.jaxrs.model.TemporaryLocationObject;
import org.folio.rest.jaxrs.model.Reserve;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import org.junit.Test;

public class UtilTest {

  public static final Logger logger = LoggerFactory.getLogger(UtilTest.class);

  @Test
  public void testPojoFromJson() throws Exception {
    LocationObject locationObject = new LocationObject();
    String name = "Front Desk";
    String id = UUID.randomUUID().toString();
    JsonObject json = new JsonObject();
    json.put("id", id);
    json.put("name", name);
    json.put("isActive", Boolean.TRUE);
    JsonArray servicePointIds = new JsonArray();
    servicePointIds.add(UUID.randomUUID().toString());
    servicePointIds.add(UUID.randomUUID().toString());
    servicePointIds.add(UUID.randomUUID().toString());
    json.put("servicePointIds", servicePointIds);
    List<PopulateMapping> mapList = new ArrayList<>();
    mapList.add(new PopulateMapping("id"));
    mapList.add(new PopulateMapping("name"));
    mapList.add(new PopulateMapping("isActive", PopulateMapping.ImportType.BOOLEAN));
    mapList.add(new PopulateMapping("servicePointIds", PopulateMapping.ImportType.STRINGLIST));
    CRUtil.populatePojoFromJson(locationObject, json, mapList);
    assertTrue(locationObject.getName().equals(name));
    assertTrue(locationObject.getId().equals(id));
    assertTrue(locationObject.getIsActive());
    assertTrue(locationObject.getServicePointIds().size() == 3);
  }

  @Test
  public void testCopyFields() {
    TemporaryLocationObject tempLocationObject = new TemporaryLocationObject();
    LocationObject locationObject = new LocationObject();
    locationObject.setId(UUID.randomUUID().toString());
    locationObject.setName("Big Library");
    locationObject.setIsActive(Boolean.TRUE);
    CRUtil.copyFields(tempLocationObject, locationObject);
    assertTrue(tempLocationObject.getId().equals(locationObject.getId()));
    assertTrue(tempLocationObject.getName().equals(locationObject.getName()));
    assertTrue(tempLocationObject.getIsActive().equals(locationObject.getIsActive()));
  }

  @Test
  public void testNullCopyFields() {
    TemporaryLocationObject tempLocationObject = new TemporaryLocationObject();
    CRUtil.copyFields(tempLocationObject, null);
    assertNull(tempLocationObject.getId());
  }

  @Test
  public void testReserfListFromReserveList() {
    List<Reserve> reserveList = new ArrayList<>();
    Reserve reserve = new Reserve();
    reserve.setId(UUID.randomUUID().toString());
    reserveList.add(reserve);
    List<Reserf> reserfList = CourseAPI.reserfListFromReserveList(reserveList);
    assertEquals(reserfList.get(0).getId(), reserve.getId());
  }

  @Test
  public void testGetStringValuesFromObjectArrays() {
    JsonArray array = new JsonArray();
    array.add(new JsonObject().put("dog", "woof"));
    assertEquals(CRUtil.getStringValueFromObjectArray("dog", array), "woof");
    assertNull(CRUtil.getStringValueFromObjectArray("cat", array));
  }

  @Test
  public void testLogAndSaveError() {
    String ahhString = "AAAAAAAHHHH";
    try {
      throw new Exception(ahhString);
    } catch(Exception e) {
      String message = Util.logAndSaveError(e, logger);
      assertTrue(message.contains(ahhString));
    }
  }
}
