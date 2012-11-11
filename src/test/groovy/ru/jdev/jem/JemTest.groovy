package ru.jdev.jem

import spock.lang.Specification
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig
import com.google.appengine.tools.development.testing.LocalServiceTestHelper

import com.google.gson.JsonParser
import com.google.appengine.api.datastore.DatastoreServiceFactory

import static ru.jdev.jem.ScalaTestUtils.*
import com.google.appengine.api.datastore.Key

class JemTest extends Specification {

    static LocalServiceTestHelper helper

    def setupSpec() {

        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())
        helper.setUp()
    }
    def cleanupSpec() {
        helper.tearDown()
    }

    def "Storing a empty json object"() {
        when: "Empty json object is stored"
        def jem = new JEM(SList([]))
        def key = jem.store(new JsonParser().parse("{}").asJsonObject, "User", null)

        then: "It can be retreived by it's key and do not have properties"
        final user = DatastoreServiceFactory.getDatastoreService().get(key)
        user != null
        user.properties.size() == 0
    }

    def "Storing a json object with array"() {
        when: "Json object with 'arr' property of array type is stored"
        def jem = new JEM(SList(["arr"]))
        def key = jem.store(new JsonParser().parse("{\"arr\": [\"1\", \"2\", \"3\"]}").asJsonObject, "User", null)

        then: "It can be retreived by it's key and do have 'arr' property with 3 elements"
        final user = DatastoreServiceFactory.getDatastoreService().get(key)
        user != null
        user.properties.size() == 1
        user.properties['arr'].size() == 3
        user.properties['arr'][0] == "1"
        user.properties['arr'][1] == "2"
        user.properties['arr'][2] == "3"
    }

    def "Storing a json object with array of arrays"() {
        when: "Json object with 'arr' property of array of arrays type is stored"
        def jem = new JEM(SList(["arr"]))
        jem.store(new JsonParser().parse("{\"arr\": [[\"1\"], [\"2\"], [\"3\"]]}").asJsonObject, "User", null)

        then: "IllegalArgumentException must be throwned"
        thrown IllegalArgumentException
    }

    def "Storing a json object with indexed and unindexed properties"() {
        when: "Json object with 'arr' property of array of arrays type is stored"
        def json = """
            {
                "indexedProp": "indexedProp",
                "indexedArr": ["1", "2", "3"],
                "indexedObj": { "one": "1", "two": "2", "three": "3", "indexedProp": "4"},

                "unindexedProp": "unindexedProp",
                "unindexedArr": ["1", "2", "3"],
                "unindexedObj": { "one": "1", "two": "2", "three": "3"}
            }
        """
        def jem = new JEM(SList(["indexedProp", "indexedArr", "indexedObj"]))
        def key = jem.store(new JsonParser().parse(json).asJsonObject, "User", null)

        then: "They must be retreived"
        final user = DatastoreServiceFactory.getDatastoreService().get(key)
        user != null
        user.properties.size() == 6

        user.isUnindexedProperty('unindexedProp')
        user.isUnindexedProperty('unindexedArr')
        user.isUnindexedProperty('unindexedObj')

        user.properties['unindexedProp'] == '"unindexedProp"'
        user.properties['unindexedArr'] == '["1","2","3"]'
        user.properties['unindexedObj'] == '{"one":"1","two":"2","three":"3"}'

        !user.isUnindexedProperty('indexedProp')
        !user.isUnindexedProperty('indexedArr')
        !user.isUnindexedProperty('indexedObj')

        user.properties['indexedProp'] == 'indexedProp'
        user.properties['indexedArr'] == ["1", "2", "3"]
        user.properties['indexedObj'] instanceof Key
        def subObj = DatastoreServiceFactory.getDatastoreService().get((Key)user.properties['indexedObj'])

        subObj.properties.size() == 4
        subObj.properties['one'] == '"1"'
        subObj.properties['two'] == '"2"'
        subObj.properties['three'] == '"3"'
        subObj.properties['indexedProp'] == '4'
    }

}
