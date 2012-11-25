package ru.jdev.jem

import com.google.appengine.api.datastore.KeyFactory

class JemCollectionFactory(indexedFields: Iterable[String], kind: String, idField: String) {

  def withoutParent: JemCollection = new JemCollection(new JemConfiguration(indexedFields, kind, null, idField))

  def withParent(parent: String): JemCollection = new JemCollection(new JemConfiguration(indexedFields, kind, KeyFactory.stringToKey(parent), idField))

}

object JemCollectionFactory {

  def collectionFor(kind: String, indexedFields: Iterable[String], idField: String = "_id"): JemCollectionFactory = new JemCollectionFactory(indexedFields, kind, idField)

}
