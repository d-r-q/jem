package ru.jdev.jem

import spock.lang.Specification
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig
import com.google.appengine.tools.development.testing.LocalServiceTestHelper

import com.google.gson.JsonParser
import com.google.appengine.api.datastore.DatastoreServiceFactory

import static ru.jdev.jem.ScalaTestUtils.*
import com.google.appengine.api.datastore.Key
import scala.Some
import scala.collection.immutable.Nil
import com.google.appengine.api.datastore.KeyFactory

import static ru.jdev.jem.JemCollectionFactory.collectionFor
import static com.google.appengine.api.datastore.KeyFactory.stringToKey
import com.google.gson.JsonObject
import com.google.gson.JsonNull

class JemTest extends Specification {

    private static LocalServiceTestHelper helper

    def setupSpec() {

        helper = new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig())
        helper.setUp()
    }

    def cleanupSpec() {
        helper.tearDown()
    }

    def "Storing a empty json object"() {

        when: "Empty json object is stored"

        final users = collectionFor("User", SList([]), "_id").withoutParent()
        final key = users.store(new JsonParser().parse("{}").asJsonObject)

        then: "It can be retreived by it's key and do not have properties"

        final user = DatastoreServiceFactory.getDatastoreService().get(stringToKey(key))
        user != null
        user.properties.size() == 0
    }

    def "Storing a json object with array"() {

        when: "Json object with 'roles' property of array type is stored"

        final rolesField = 'roles'
        final users = collectionFor('User', SList([rolesField]), '_id').withoutParent()
        final key = users.store(new JsonParser().parse('{"' + rolesField + '":["user","admin","guest"]}').asJsonObject)

        then: "It can be retreived by it's key and do have 'arr' property with 3 elements"

        final user = DatastoreServiceFactory.getDatastoreService().get(stringToKey(key))
        user != null
        user.properties.size() == 1

        user.properties[rolesField].size() == 3

        user.properties[rolesField][0] == "user"
        user.properties[rolesField][1] == "admin"
        user.properties[rolesField][2] == "guest"
    }

    def "Storing a json object with array of arrays"() {

        when: "Json object with 'roles' property of array of arrays type is stored"

        final rolesField = 'roles'
        final users = collectionFor('User', SList([rolesField]), '_id').withoutParent()
        users.store(new JsonParser().parse('{"' + rolesField + '":[[]]}').asJsonObject)

        then: "IllegalArgumentException must be throwned"

        thrown IllegalArgumentException
    }

    def "Storing a json object with indexed and unindexed properties"() {

        when: "Json object with indexed and unindexed properties is stored"

        final json = """
                {
                    "login": "test",
                    "roles": ["guest", "user", "admin"],
                    "config": { "locale": "ru", "remember": "true", "login": "test"},

                    "name": "Test Test",
                    "interests": ["scala", "gradle"],
                    "nextTask": { "type": "load", "url": "google.com"}
                }
            """

        final users = collectionFor("User", SList(["login", "roles", "config"]), "_id").withoutParent()
        final String key = users.store(new JsonParser().parse(json).asJsonObject)

        then: "They must be retreived"

        final user = DatastoreServiceFactory.getDatastoreService().get(stringToKey(key))
        user != null
        user.properties.size() == 6

        user.isUnindexedProperty('name')
        user.isUnindexedProperty('interests')
        user.isUnindexedProperty('nextTask')

        user.properties['name'] == 'Test Test'
        user.properties['interests'] == ['scala', 'gradle']
        user.properties['nextTask'] instanceof Key

        final nextTask = DatastoreServiceFactory.getDatastoreService().get(user.properties['nextTask'])
        nextTask != null
        nextTask.properties.size() == 2

        nextTask.isUnindexedProperty('type')
        nextTask.isUnindexedProperty('url')

        nextTask.properties['type'] == 'load'
        nextTask.properties['url'] == 'google.com'

        !user.isUnindexedProperty('login')
        !user.isUnindexedProperty('roles')
        !user.isUnindexedProperty('config')

        user.properties['login'] == 'test'
        user.properties['roles'] == ["guest", "user", "admin"]

        user.properties['config'] instanceof Key
        final Key cfgKey = user.properties['config'] as Key
        cfgKey.getKind() == "User.inner"
        final config = DatastoreServiceFactory.getDatastoreService().get(cfgKey)
        config != null
        config.properties.size() == 3

        config.isUnindexedProperty('locale')
        config.isUnindexedProperty('remember')
        config.isUnindexedProperty('login')

        config.properties['locale'] == 'ru'
        config.properties['remember'] == 'true'
        config.properties['login'] == 'test'
    }

    def "Storing and loading simple object"() {

        final users = collectionFor("User", SList(["login"]), "_id").withoutParent()
        def String key

        when: "Simple object is stored"

        key = users.store(new JsonParser().parse('{"login":"test","name":"Test Test"}').asJsonObject)

        then: "It can be loaded by it's key"

        def jsonObjOpt = users.load(key)

        jsonObjOpt instanceof Some
        def jsonObj = jsonObjOpt.get()
        jsonObj.entrySet().size() == 2

        jsonObj.get('login').getAsString() == 'test'
        jsonObj.get('name').getAsString() == 'Test Test'
    }

    def "Storing object with 3 leveles of TBD"() {
        final roots = collectionFor("Root", SList([]), "_id").withoutParent()

        when: "Object with 3 leveles of TBD is stored"
        final String key = roots.store(new JsonParser().parse("""{
        "inner1":{
            "inner2":{
                "value":"value"
            }
        }
        }""").asJsonObject)

        then: "Inner objects has same category"
        final root = DatastoreServiceFactory.getDatastoreService().get(stringToKey(key))
        root != null
        root.properties.size() == 1
        root.isUnindexedProperty('inner1')
        root.properties['inner1'] instanceof Key

        final inner1Key = root.properties['inner1'] as Key
        inner1Key.getKind() == "Root.inner"
        final inner1 = DatastoreServiceFactory.getDatastoreService().get(inner1Key)
        inner1 != null
        inner1.properties.size() == 1

        inner1.isUnindexedProperty('inner2')
        inner1.properties['inner2'] instanceof Key

        final inner2Key = inner1.properties['inner2'] as Key
        inner2Key.getKind() == "Root.inner"
        final inner2 = DatastoreServiceFactory.getDatastoreService().get(inner2Key)
        inner2 != null
        inner2.properties.size() == 1
        inner2.isUnindexedProperty('value')
        inner2.properties['value'] == 'value'
    }

    def "Storing object with null"() {
        final roots = collectionFor("Root", SList([]), "_id").withoutParent()
        when: "Object with property set to null is stored"
        final String key = roots.store(new JsonParser().parse('{"nullProp":null}').asJsonObject)

        then: "It must be retreived as JsonNull object"
        final rootOpt = roots.load(key)

        rootOpt != null
        rootOpt instanceof Some

        final JsonObject root = rootOpt.get()
        root.get("nullProp") instanceof JsonNull

    }

}