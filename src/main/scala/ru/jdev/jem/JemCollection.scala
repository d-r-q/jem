package ru.jdev.jem

import com.google.appengine.api.datastore.{Entity, DatastoreServiceFactory, Key, Query}
import com.google.gson._
import collection.JavaConversions._
import scala.Some
import java.util

import com.google.appengine.api.datastore.KeyFactory.stringToKey
import com.google.appengine.api.datastore.KeyFactory.keyToString
import collection.immutable

class JemCollection(val cfg: JemConfiguration) {

  type EntityKeyString = String

  private type FieldSetter[Src, Dst] = (Src, Dst, String) => Unit

  private val indexedProps = cfg.indexedProps
  private val idProp = cfg.idProp
  private val parent = cfg.parent
  private val kind = cfg.kind

  private val gson = new Gson

  private val jsonSetter = (source: Entity, dest: JsonObject, propName: String) =>
    dest.add(propName, getJsonProperty(source.getProperty(propName)))

  private val entitySetter = (source: JsonObject, dest: Entity, propName: String) =>
    if (indexedProps.contains(propName)) {
      dest.setProperty(propName, getEntityProperty(dest, source.get(propName)))
    } else {
      dest.setUnindexedProperty(propName, getEntityProperty(dest, source.get(propName)))
    }

  def add(jsonObj: JsonObject): EntityKeyString = store(jsonObj, kind, parent)

  private def store(jsonObj: JsonObject, kind: String, parent: Key): EntityKeyString = {
    val entity = jsonObj.get(idProp) match {
      case id: JsonPrimitive if id.isNumber => getEntityByKey(id.getAsString)
      case null => {
        val newEntity: Entity = new Entity(kind, parent)
        dataStore.put(newEntity)
        newEntity
      }
      case _ => throw new IllegalArgumentException("Invalid id value = " + gson.toJson(jsonObj.get(idProp)))
    }

    keyToString(dataStore.put(copyJsonProperties(jsonObj, entity)))
  }

  def load(key: EntityKeyString): Option[JsonObject] = getEntityByKey(key) match {
    case entity: Entity => Some(copyEntityProperties(entity))
    case null => None
  }

  def load(query: Query): Iterable[JsonObject] = dataStore.prepare(query).asIterable.map(copyEntityProperties(_))

  def loadAll = dataStore.prepare(new Query(kind, parent)).asIterable().map(copyEntityProperties(_))

  def delete(key: EntityKeyString) {
    dataStore.delete(stringToKey(key))
  }

  def getEntityByKey(key: EntityKeyString) = dataStore.get(stringToKey(key))

  private def dataStore = DatastoreServiceFactory.getDatastoreService

  private def copyProperties[Src, Dst](setter: FieldSetter[Src, Dst], src: Src, dst: Dst, names: TraversableOnce[String]) =
    names.foldLeft(dst) {
      (dst: Dst, name: String) => {
        setter(src, dst, name)
        dst
      }
    }

  private def copyEntityProperties(src: Entity) =
    copyProperties[Entity, JsonObject](jsonSetter, src, new JsonObject, src.getProperties.keySet)

  private def copyJsonProperties(src: JsonObject, dst: Entity) =
    copyProperties[JsonObject, Entity](entitySetter, src, dst, src.entrySet().map(_.getKey))

  private def getEntityProperty(parent: Entity, value: JsonElement): AnyRef = value match {
    case v: JsonNull => null
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => stringToKey(store(v, parent.getKind + ".inner", parent.getKey)) // TODO: add test
    case v: JsonArray => bufferAsJavaList(v.getAsJsonArray.iterator().map(getEntityProperty(parent, _)).toBuffer)

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