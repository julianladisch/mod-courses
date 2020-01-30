package UnitTest;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.folio.coursereserves.util.CRUtil;
import org.folio.coursereserves.util.PopulateMapping;
import org.folio.coursereserves.util.PopulateMapping.ImportType;
import org.folio.rest.jaxrs.model.LocationObject;
import org.folio.rest.jaxrs.model.LocationObject;
import org.folio.rest.jaxrs.model.TemporaryLocationObject;
import static org.junit.Assert.assertTrue;
import org.junit.Test;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertTrue;

public class UnitTest {
  
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
    mapList.add(new PopulateMapping("isActive", ImportType.BOOLEAN));
    mapList.add(new PopulateMapping("servicePointIds", ImportType.STRINGLIST));
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


  
}
