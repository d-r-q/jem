package ru.jdev.jem

import junit.framework.TestCase
import com.google.appengine.tools.development.testing.{LocalDatastoreServiceTestConfig, LocalServiceTestHelper}

import ru.jdev.jem.JemCollectionFactory.collectionFor
import com.google.gson.JsonObject

class JemUsageTest extends TestCase {

  private val helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())

  override def setUp() {
    helper.setUp()
  }

  def testUsage() {
    val users = collectionFor("User", Set()).withoutParent
    val userKey = users.store(new JsonObject)

    val posts = collectionFor("Post", Set())
    val userPosts = posts.withParent(userKey)
    userPosts.store(new JsonObject)
  }

  override def tearDown() {
    helper.tearDown()
  }

}
