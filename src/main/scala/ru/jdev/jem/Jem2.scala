package ru.jdev.jem

import com.google.appengine.api.datastore.{Entity, DatastoreServiceFactory, Key, Query}
import com.google.gson._
import collection.JavaConversions._
import scala.Some
import java.util

import com.google.appengine.api.datastore.KeyFactory.stringToKey
import com.google.appengine.api.datastore.KeyFactory.keyToString
import collection.immutable

class Jem2(val cfg: JemConfiguration) {

  private type FieldSetter[Src, Dst] = (Src, Dst, String) => Unit

  private val gson = new Gson

  private val entitySetters = {
    val entityUnindexedSetter =
      (source: JsonObject, dest: Entity, key: String) =>
        dest.setUnindexedProperty(key, getEntityProperty(source.get(key)))

    val entityIndexedSetter =
      (source: JsonObject, dest: Entity, key: String) =>
        dest.setProperty(key, getEntityProperty(source.get(key)))

    cfg.indexedFields.zip(Stream.continually(entityIndexedSetter)).toMap.withDefaultValue(entityUnindexedSetter)
  }

  private val jsonSetters = immutable.Map[String, FieldSetter[Entity, JsonObject]]().withDefaultValue(
    (source: Entity, dest: JsonObject, key: String) => dest.add(key, getJsonProperty(source.getProperty(key)))
  )

  def store(jsonObj: JsonObject): String = {
    val entity = jsonObj.get(cfg.idField) match {
      case id: JsonPrimitive if id.isNumber => getEntityByKey(id.getAsString)
      case null => {
        val newEntity: Entity = new Entity(cfg.kind, cfg.parent)
        dataStore.put(newEntity)
        newEntity
      }
      case _ => throw new IllegalArgumentException("Invalid id value = " + gson.toJson(jsonObj.get(cfg.idField)))
    }

    keyToString(dataStore.put(copyJsonProperties(jsonObj, entity)))
  }

  def load(key: String): Option[JsonObject] = getEntityByKey(key) match {
    case entity: Entity => Some(copyEntityProperties(entity))
    case null => None
  }

  def load(query: Query): Iterable[JsonObject] = dataStore.prepare(query).asIterable.map(copyEntityProperties(_))

  def loadAll = dataStore.prepare(new Query(cfg.kind, cfg.parent)).asIterable().map(copyEntityProperties(_))

  def delete(key: String) {
    dataStore.delete(stringToKey(key))
  }

  def getEntityByKey(key: String) = dataStore.get(stringToKey(key))

  private def dataStore = DatastoreServiceFactory.getDatastoreService

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

  private def getEntityProperty(value: JsonElement): AnyRef = value match {
    case v: JsonNull => null
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => stringToKey(store(v))
    case v: JsonArray => bufferAsJavaList(v.getAsJsonArray.iterator().map(getEntityProperty).toBuffer)

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

  private def getJsonProperty(value: AnyRef): JsonElement = value match {
    case v: AnyRef if v eq null => JsonNull.INSTANCE // TODO: add test
    case v: String => new JsonPrimitive(v)
    case v: Key => load(keyToString(v)) match {
      case Some(element) => element
      case None => JsonNull.INSTANCE // TODO: consistency problem?
    }
    case v: util.Collection[AnyRef] => {
      val res = new JsonArray
      v.map(getJsonProperty).foreach {
        res.add(_)
      }
      res
    }

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

}