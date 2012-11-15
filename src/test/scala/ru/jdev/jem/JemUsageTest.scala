package ru.jdev.jem

import junit.framework.TestCase
import com.google.gson.JsonParser
import org.junit.Assert
import com.google.appengine.tools.development.testing.{LocalDatastoreServiceTestConfig, LocalServiceTestHelper}

class JemUsageTest extends TestCase {

  private val helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())

  override def setUp() {
    helper.setUp()
  }

  def testUsage() {
    val jem = new JEM(Set("indexed"), kind = "Test")
    val jsonStr: String = """{"indexed":"indexed","unindexed":"unindexed"}"""
    val key = jem.store(new JsonParser().parse(jsonStr).getAsJsonObject)
    jem.load(key.getId) match {
      case Some(json) => {
        Assert.assertEquals("indexed", json.getAsJsonPrimitive("indexed").getAsString)
        Assert.assertEquals("unindexed", json.getAsJsonPrimitive("unindexed").getAsString)
        Assert.assertEquals(jsonStr, json.toString)
      }
      case None => Assert.fail("Stored object was not loaded")
    }

  }

  override def tearDown() {
    helper.tearDown()
  }

}
