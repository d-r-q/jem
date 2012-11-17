package ru.jdev.jem

import com.google.appengine.api.datastore.Key

class JemConfiguration(val indexedProps: Iterable[String], val kind: String, val parent: Key, val idProp: String = "_id");
