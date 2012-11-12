package ru.jdev.jem

import scala.collection.JavaConversions._

import com.google.appengine.api.datastore.{Entity, Key, DatastoreServiceFactory, Query}
import com.google.gson._
import com.google.appengine.api.datastore
import datastore.KeyFactory.Builder
import java.util.{Map => JMap}
import java.util

class JEM(cfg: scala.collection.Iterable[String], val idPropertyName: String = "_id") {

  private val entitySetters: Map[String, ((Entity, String, JsonElement) => Unit)] = (cfg map entitySetter).toMap

  private val jsonSetters: Map[String, ((JsonObject, String, Entity) => Unit)] = (cfg map jsonSetter).toMap

  private val gson = new Gson

  private def dataStore = DatastoreServiceFactory.getDatastoreService

  def store(obj: JsonObject, kind: String, parent: Key): Key = {
    val builder = new Builder(parent)
    val entity = obj.get(idPropertyName) match {
      case id: JsonPrimitive if id.isNumber => dataStore.get(builder.addChild(kind, id.getAsLong).getKey)
      case null => {
        val newEntity: Entity = new Entity(kind, parent)
        dataStore.put(newEntity)
        newEntity
      }
      case _ => throw new IllegalArgumentException("Invalid _id value = " + gson.toJson(obj.get(idPropertyName)))
    }

    obj.entrySet().foreach((entry: JMap.Entry[String, JsonElement]) => {
      entitySetters.getOrElse(entry.getKey, entityUnindexedSetter _)(entity, entry.getKey, entry.getValue)
    })

    dataStore.put(entity)
  }


  private def getEntityProperty(parent: Key)(value: JsonElement): AnyRef = value match {
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => store(v, parent.getKind, parent)
    case v: JsonArray => {
      val javaList: java.util.List[AnyRef] = v.getAsJsonArray.iterator().map(getEntityProperty(parent)).toList
      javaList
    }

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

  private def entityIndexedSetter(entity: Entity, key: String, value: JsonElement) {
    entity.setProperty(key, getEntityProperty(entity.getKey)(value))
  }

  private def entityUnindexedSetter(entity: Entity, key: String, value: JsonElement) {
    entity.setUnindexedProperty(key, value.toString)
  }

  private def entitySetter(field: String): (String, ((Entity, String, JsonElement) => Unit)) = (field, entityIndexedSetter)

  def load(id: Long, kind: String, parent: Key): Option[JsonObject] =
    convertEntityToJson(dataStore.get(new Builder(parent).addChild(kind, id).getKey))


  def convertEntityToJson(entity: Entity) = entity match {
    case entity: Entity => {
      val jsonObj = new JsonObject

      entity.getProperties.foreach((property: (String, AnyRef)) => {
        jsonSetters.getOrElse(property._1, jsonUnindexedSetter _)(jsonObj, property._1, entity)
      })

      Some(jsonObj)
    }

    case null => None
  }

  private def getJsonProperty(parent: Key)(value: AnyRef): JsonElement = value match {
    case v: String => new JsonPrimitive(v)
    case v: Key => load(v.getId, parent.getKind, parent) match {
      case Some(element) => element
      case None => JsonNull.INSTANCE
    }
    case v: util.Collection[AnyRef] => {
      val res = new JsonArray
      (v map getJsonProperty(parent)) foreach {
        res.add(_)
      }
      res
    }
  }

  private def jsonIndexedSetter(json: JsonObject, key: String, entity: Entity) {
    json.add(key, getJsonProperty(entity.getKey)(entity.getProperty(key)))
  }

  private def jsonUnindexedSetter(json: JsonObject, key: String, value: Entity) {
    json.add(key, new JsonParser().parse(value.getProperty(key).asInstanceOf[String]))
  }

  private def jsonSetter(field: String): (String, ((JsonObject, String, Entity) => Unit)) = (field, jsonIndexedSetter)

  def load(query: Query) = dataStore.prepare(query).asIterable() map convertEntityToJson

}
