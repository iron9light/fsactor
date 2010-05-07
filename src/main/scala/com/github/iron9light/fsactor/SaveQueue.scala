/**
 * Copyright 2010 iron9light
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.github.iron9light.fsactor

import annotation.tailrec

private[fsactor] class SaveQueue {
  def size: Int = _size

  private var _size = 0

  private val head = Node()
  private var end = head

  def enqueue(message: Any) {
    val node = Node(message)
    end.next = node
    end = node
    _size += 1
  }

  def apply(act: PartialFunction[Any, Unit]): Boolean = {
    @tailrec
    def process(node: Node): Boolean = {
      val nextNode = node.next

      if (nextNode == Null)
        false
      else if (act isDefinedAt nextNode.value) {
        // remove nextNode
        node.next = nextNode.next
        if (end == nextNode) // nextNode is end
          end = node
        _size -= 1

        act(nextNode.value)

        true
      } else
        process(nextNode)
    }

    process(head)
  }

  def flush() {
    head.next = Null
    end = head
    _size = 0
  }

  private object Node {
    def apply() = new Node(null, Null)

    def apply(value: Any) = new Node(value, Null)
  }
  private class Node(val value: Any, var next: Node)

  private object Null extends Node(null, null)
}