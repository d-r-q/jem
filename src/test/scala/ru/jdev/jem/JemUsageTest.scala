package ru.jdev.jem

import junit.framework.TestCase
import com.google.appengine.tools.development.testing.{LocalDatastoreServiceTestConfig, LocalServiceTestHelper}

import ru.jdev.jem.JemFactoryBuilder.createFactoryFor

class JemUsageTest extends TestCase {

  private val helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())

  override def setUp() {
    helper.setUp()
  }

  def testUsage() {
    val usersFactory = createFactoryFor("User", Set())
    val users = usersFactory.withoutParent
    val userKey = users.store(null)

    val userPostsFactory = createFactoryFor("Post", Set())
    val userPosts = userPostsFactory.withParent(userKey)
    userPosts
  }

  override def tearDown() {
    helper.tearDown()
  }

}
