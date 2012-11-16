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
    val roots = Jem2.factoryFor("Test root", Set("indexed"))()
    val rootKey = roots.store(null)

    val childrenFactory = Jem2.factoryFor("Test child", Set("indexed"))
    val rootChildren = childrenFactory(rootKey)

    val childKey = rootChildren.store(null)

    rootChildren.delete(childKey)

  }

  override def tearDown() {
    helper.tearDown()
  }

}
