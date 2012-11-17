package ru.jdev.jem

import junit.framework.TestCase
import com.google.appengine.tools.development.testing.{LocalDatastoreServiceTestConfig, LocalServiceTestHelper}

import ru.jdev.jem.JemCollectionFactory.collectionFor

class JemUsageTest extends TestCase {

  private val helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())

  override def setUp() {
    helper.setUp()
  }

  def testUsage() {
    val users = collectionFor("User", Set()).withoutParent
    val userKey = users.add(null)

    val posts = collectionFor("Post", Set())
    val userPosts = posts.withParent(userKey)
    userPosts
  }

  override def tearDown() {
    helper.tearDown()
  }

}
