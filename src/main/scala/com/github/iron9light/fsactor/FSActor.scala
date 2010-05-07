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

import se.scalablesolutions.akka.actor.Actor

object FSActor {
  def apply(body: PartialFunction[Any, Unit]) = new FSActor {
    currentAct = body
  }
}

abstract class FSActor extends Actor {
  private val saveQueue: SaveQueue = new SaveQueue

  protected var currentAct: PartialFunction[Any, Unit] = {case _ if (false) =>}

  protected final def receive: PartialFunction[Any, Unit] = currentAct andThen applySaveQueue orElse saveMessage

  override def mailboxSize = super.mailboxSize + saveQueue.size

  override def postRestart(reason: scala.Throwable) {
    super.postRestart(reason)
    while (saveQueue(currentAct)) {}
  }

  def saveQueueSize = saveQueue.size

  protected def changeTo(state: PartialFunction[Any, Unit]) {
    currentAct = state
  }

  protected def react(body: PartialFunction[Any, Unit]) = changeTo(body)

  protected def flush() {saveQueue.flush()}

  private def saveMessage: PartialFunction[Any, Unit] = {case message => saveQueue.enqueue(message)}

  private def applySaveQueue(x: Unit) {
    while (saveQueue(currentAct)) {}
  }
}