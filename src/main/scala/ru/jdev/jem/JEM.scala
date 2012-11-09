package ru.jdev.jem

import scala.collection.JavaConversions._

import com.google.appengine.api.datastore.{Entity, Key, DatastoreServiceFactory}
import com.google.gson._
import com.google.appengine.api.datastore
import datastore.KeyFactory.Builder
import java.util.{Map => JMap}

class JEM(cfg: Set[String]) {

  private val setters: Map[String, ((Entity, String, JsonElement) => Unit)] = (cfg map setter).toMap

  private val gson = new Gson

  private def dataStore = DatastoreServiceFactory.getDatastoreService

  def store(obj: JsonObject, kind: String, parent: Key): Key = {
    val builder: Builder = new Builder(parent)
    val entity = obj.get("_id") match {
      case id: JsonPrimitive if id.isNumber => dataStore.get(builder.addChild(kind, id.getAsLong).getKey)
      case null => {
        val newEntity: Entity = new Entity(kind, parent)
        dataStore.put(newEntity)
        newEntity
      }
      case _ => throw new IllegalArgumentException("Invalid _id value = " + gson.toJson(obj.get("_id")))
    }

    obj.entrySet().foreach((entry: JMap.Entry[String, JsonElement]) => {
      setters.getOrElse(entry.getKey, unindexedSetter _)(entity, entry.getKey, entry.getValue)
    })

    dataStore.put(entity)
  }


  private def getEntityProperty(parent: Key)(value: JsonElement): AnyRef = value match {
    case v: JsonArray => {
      val javaList: java.util.List[AnyRef] = v.getAsJsonArray.iterator().map(getEntityProperty(parent)).toList
      javaList
    }
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => store(v, parent.getKind, parent)
    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

  private def indexedSetter(entity: Entity, key: String, value: JsonElement) {
    entity.setProperty(key, getEntityProperty(entity.getKey)(value))
  }

  private def unindexedSetter(entity: Entity, key: String, value: JsonElement) {
    entity.setUnindexedProperty(key, value.toString)
  }

  private def setter(field: String): (String, ((Entity, String, JsonElement) => Unit)) = (field, indexedSetter)

}
