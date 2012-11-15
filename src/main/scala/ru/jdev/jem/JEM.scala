package ru.jdev.jem

import scala.collection.JavaConversions._

import com.google.appengine.api.datastore.{Entity, Key, DatastoreServiceFactory, Query}
import com.google.gson._
import com.google.appengine.api.datastore
import datastore.KeyFactory.Builder
import java.util
import scala.Predef._
import scala.AnyRef
import scala.Some
import collection.immutable

class JEM(val indexedFields: Iterable[String], val kind: String = null, val idPropertyName: String = "_id") {

  private type FieldSetter[Src, Dst] = (Src, Dst, String) => Unit
  private type EntityFieldSetter = FieldSetter[JsonObject, Entity]
  private type JsonFieldSetter = FieldSetter[Entity, JsonObject]

  private val gson = new Gson

  private val (entitySetters, jsonSetters) = {
    val entityUnindexedSetter =
      (source: JsonObject, dest: Entity, key: String) =>
        dest.setUnindexedProperty(key, source.get(key).toString)

    val entityIndexedSetter =
      (source: JsonObject, dest: Entity, key: String) =>
        dest.setProperty(key, getEntityProperty(dest.getKey)(source.get(key)))

    val jsonUnindexedSetter =
      (source: Entity, dest: JsonObject, key: String) =>
        dest.add(key, new JsonParser().parse(source.getProperty(key).asInstanceOf[String]))

    val jsonIndexedSetter =
      (source: Entity, dest: JsonObject, key: String) =>
        dest.add(key, getJsonProperty(source.getKey)(source.getProperty(key)))

    (indexedFields.zip(Stream.continually(entityIndexedSetter)).toMap.withDefaultValue(entityUnindexedSetter),
      indexedFields.zip(Stream.continually(jsonIndexedSetter)).toMap.withDefaultValue(jsonUnindexedSetter))
  }

  def store(jsonObj: JsonObject, parent: Key = null, kind: String = kind): Key = {
    val entity = jsonObj.get(idPropertyName) match {
      case id: JsonPrimitive if id.isNumber => getEntityByKey(parent, kind, id.getAsLong)
      case null => {
        val newEntity: Entity = new Entity(kind, parent)
        dataStore.put(newEntity)
        newEntity
      }
      case _ => throw new IllegalArgumentException("Invalid id value = " + gson.toJson(jsonObj.get(idPropertyName)))
    }

    dataStore.put(copyJsonProperties(jsonObj, entity))
  }

  def load(id: Long, parent: Key = null, kind: String = kind): Option[JsonObject] = getEntityByKey(parent, kind, id) match {
    case entity: Entity => Some(copyEntityProperties(entity))
    case null => None
  }

  def load(query: Query) = dataStore.prepare(query).asIterable map(copyEntityProperties(_))

  private def dataStore = DatastoreServiceFactory.getDatastoreService

  private def getEntityByKey(parent: Key, kind: String, id: Long) = dataStore.get(new Builder(parent).addChild(kind, id).getKey)

  private def copyProperties[Src, Dst](setters: immutable.Map[String, FieldSetter[Src, Dst]])(src: Src, dst: Dst, names: TraversableOnce[String]) =
    names.foldLeft(dst) {
      (dst: Dst, name: String) => {
        setters(name)(src, dst, name)
        dst
      }
    }

  private def copyEntityProperties(src: Entity) =
    copyProperties[Entity, JsonObject](jsonSetters)(src, new JsonObject, src.getProperties.keySet)

  private def copyJsonProperties(src: JsonObject, dst: Entity) =
    copyProperties[JsonObject, Entity](entitySetters)(src, dst, src.entrySet().map(_.getKey))

  private def getEntityProperty(parent: Key)(value: JsonElement): AnyRef = value match {
    // TODO: add case and test for null
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => store(v, parent, parent.getKind)
    case v: JsonArray => bufferAsJavaList(v.getAsJsonArray.iterator().map(getEntityProperty(parent)).toBuffer)

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

  private def getJsonProperty(parent: Key)(value: AnyRef): JsonElement = value match {
    case v: String => new JsonPrimitive(v)
    case v: Key => load(v.getId, parent, parent.getKind) match {
      case Some(element) => element
      case None => JsonNull.INSTANCE // TODO: consistency problem?
    }
    case v: util.Collection[AnyRef] => {
      val res = new JsonArray
      v.map(getJsonProperty(parent)).foreach {
        res.add(_)
      }
      res
    }
    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

}