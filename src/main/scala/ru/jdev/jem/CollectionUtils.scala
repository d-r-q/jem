package ru.jdev.jem

object CollectionUtils {

  def fill[Container, Element](container: Container, elements: TraversableOnce[Element], fill: (Container, Element) => Unit): Container = {
    elements.foreach(fill(container, _))
    container
  }
}
