package io.mesosphere.mesosaurus

/**
 * Doubly linked list that forms a ring.
 * Good for:
 * - sequential traversal in order of element addition 
 * - quick element removal at any list position
 * 
 * Each element holds a value of type \code{V}.
 */
class Ring[V](val value :V) {

  private var _previous = this

  /**
   * @return the ring element that precedes this
   */
  def previous(): Ring[V] = {
    return _previous
  }

  private var _next = this

  /**
   * @return the ring element next after this
   */
  def next(): Ring[V] = {
    return _next
  }

  /**
   * Add a ring element with the given \code{value} to the end of the list,
   * i.e. as next element after the last so far,
   * and as previous element to this.
   */
  def add(value :V) = {
	val r = new Ring[V](value);
    r._next = this;
    r._previous = _previous;
    _previous = r;
    r._previous._next = r;
  }

  /**
   * Remove this element from the list of multiple elements it is in, if any.
   * If this is not the last element in its list, remove it, 
   * then return the element that was next in the list.
   * If this is the last element, do nothing, and return it.
   */
  def remove(): Ring[V] = {
    val p = _previous;
    val n = _next;
    p._next = n;
    n._previous = p;
    _previous = this;
    _next = this;
    return n;
  }

}
