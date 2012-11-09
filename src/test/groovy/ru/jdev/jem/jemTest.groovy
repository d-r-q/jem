package ru.jdev.jem

import spock.lang.Specification
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig
import com.google.appengine.tools.development.testing.LocalServiceTestHelper

import com.google.gson.JsonParser
import com.google.appengine.api.datastore.DatastoreServiceFactory

class jemTest extends Specification {

    static LocalServiceTestHelper helper

    def setupSpec() {

        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())
        helper.setUp()
    }
    def cleanupSpec() {
        helper.tearDown()
    }

    def "Storing a empty json object"() {
/*        when: "Object empty is stored"
        def jem = new JEM(ru.jdev.jem.ScalaTestUtils.SSet())
        def key = jem.store(new JsonParser().parse("{}").asJsonObject, "User", null)

        then: "It can be retreived by it's key and do not have properties"
        final user = DatastoreServiceFactory.getDatastoreService().get(key)
        user != null
        user.properties.size() == 0*/
    }

    def "Storing a json object with array"() {
/*        when: "Object object with arra is stored"
        def jem = new JEM(ru.jdev.jem.ScalaTestUtils.SSet(["arr"] as Set))
        def key = jem.store(new JsonParser().parse("{\"arr\": [\"1\", \"2\", \"3\"]}").asJsonObject, "User", null)

        then: "It can be retreived by it's key and do have arr propertiy"
        final user = DatastoreServiceFactory.getDatastoreService().get(key)
        user != null
        user.properties.size() == 1
        user.properties['arr'].size() == 3
        user.properties['arr'][0] == "1"
        user.properties['arr'][1] == "2"
        user.properties['arr'][2] == "3"*/
    }

}
