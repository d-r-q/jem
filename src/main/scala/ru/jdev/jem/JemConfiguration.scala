package ru.jdev.jem

import com.google.appengine.api.datastore.Key

class JemConfiguration(val indexedFields: Iterable[String] = Set(), val kind: String, val parent: Key, val idField: String = "_id");
