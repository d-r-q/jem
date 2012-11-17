package ru.jdev.jem

import com.google.appengine.api.datastore.KeyFactory

class JemFactoryBuilder(indexedFields: Iterable[String], kind: String, idField: String) {

  def withoutParent: Jem2 = new Jem2(new JemConfiguration(indexedFields, kind, null, idField))

  def withParent(parent: String): Jem2 = new Jem2(new JemConfiguration(indexedFields, kind, KeyFactory.stringToKey(parent), idField))

}

object JemFactoryBuilder {

  def createFactoryFor(kind: String, indexedFields: Iterable[String], idField: String = "_id"): JemFactoryBuilder = new JemFactoryBuilder(indexedFields, kind, idField)

}
