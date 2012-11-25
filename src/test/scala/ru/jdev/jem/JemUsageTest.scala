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
    val usersConfig = Set()
    val newUser: JsonObject = new JsonObject
    val postsConfig: Set[Nothing] = Set()


    val users = collectionFor("User", usersConfig).withoutParent
    val userKey = users.store(newUser)

    val posts = collectionFor("Post", postsConfig)
    val userPosts = posts.withParent(userKey)
    val newPost: JsonObject = new JsonObject
    userPosts.store(newPost)
  }

  override def tearDown() {
    helper.tearDown()
  }

}
