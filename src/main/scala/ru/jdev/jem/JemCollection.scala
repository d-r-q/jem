package ru.jdev.jem

import com.google.appengine.api.datastore.{Entity, DatastoreServiceFactory, Key, Query}
import com.google.gson._
import collection.JavaConversions._
import scala.Some
import java.util

import com.google.appengine.api.datastore.KeyFactory.stringToKey
import com.google.appengine.api.datastore.KeyFactory.keyToString

class JemCollection(val cfg: JemConfiguration) {

  type EntityKeyString = String

  private type PropertySetter[Src, Dst] = (Src, Dst, String) => Unit

  private val indexedProps = cfg.indexedProps.toSet
  private val idProp = cfg.idProp
  private val parent = cfg.parent
  private val kind = cfg.kind

  private val gson = new Gson

  private val setJsonProperty = (source: Entity, dest: JsonObject, propName: String) =>
    dest.add(propName, getJsonProperty(source.getProperty(propName)))

  private val setEntityProperty = (source: JsonObject, dest: Entity, propName: String) =>
    if (kind == dest.getKind && indexedProps.contains(propName)) {
      dest.setProperty(propName, getEntityProperty(dest, source.get(propName)))
    } else {
      dest.setUnindexedProperty(propName, getEntityProperty(dest, source.get(propName)))
    }

  def store(jsonObj: JsonObject): EntityKeyString = store(jsonObj, kind, parent)

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

  private def getEntityByKey(key: EntityKeyString) = dataStore.get(stringToKey(key))

  private def dataStore = DatastoreServiceFactory.getDatastoreService

  private def copyProperties[Src, Dst](setProperty: PropertySetter[Src, Dst], src: Src, dst: Dst, names: TraversableOnce[String]) =
    CollectionUtils.fill(dst, names, (d: Dst, e: String) => setProperty(src, d, e) )

  private def copyEntityProperties(src: Entity) =
    copyProperties[Entity, JsonObject](setJsonProperty, src, new JsonObject, src.getProperties.keySet)

  private def copyJsonProperties(src: JsonObject, dst: Entity) =
    copyProperties[JsonObject, Entity](setEntityProperty, src, dst, src.entrySet().map(_.getKey))

  private def getEntityProperty(parent: Entity, value: JsonElement): AnyRef = value match {
    case v: JsonNull => null
    case v: JsonPrimitive => v.getAsString
    case v: JsonObject => stringToKey(store(v, kind + ".inner", parent.getKey))
    case v: JsonArray => bufferAsJavaList(v.getAsJsonArray.iterator().map(getEntityProperty(parent, _)).toBuffer)

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

  private def getJsonProperty(value: AnyRef): JsonElement = value match {
    case v if v == null => JsonNull.INSTANCE
    case v: String => new JsonPrimitive(v)
    case v: Key => load(keyToString(v)) match {
      case Some(element) => element
      case None => JsonNull.INSTANCE
    }
    case v: util.Collection[AnyRef] => CollectionUtils.fill(new JsonArray, v.map(getJsonProperty),
      (c: JsonArray, e: JsonElement) => c.add(e)
    )

    case _ => throw new IllegalArgumentException("Unsupported value: " + value)
  }

}