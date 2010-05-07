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

import collection.mutable.Buffer
import org.junit.{Assert, Test}
import se.scalablesolutions.akka.actor.Actor

class FSActorTest {
  @Test
  def test1() {
    val fsactor = FSActor {
      case x: Int =>
    }

    fsactor start

    Assert.assertEquals(0, fsactor.mailboxSize)
    Assert.assertEquals(0, fsactor.saveQueueSize)

    fsactor ! 3.0

    Thread.sleep(10)

    Assert.assertEquals(1, fsactor.mailboxSize)
    Assert.assertEquals(1, fsactor.saveQueueSize)

    fsactor ! 3

    Thread.sleep(10)

    Assert.assertEquals(1, fsactor.mailboxSize)
    Assert.assertEquals(1, fsactor.saveQueueSize)

    fsactor stop
  }

  @Test
  def test2() {
    val results = Buffer[Int]()
    val fsactor = new FSActor {
      currentAct = act1

      def act1: PartialFunction[Any, Unit] = {
        case 1 =>
          results.append(1)
          changeTo(act2)
      }

      def act2: PartialFunction[Any, Unit] = {
        case 2 =>
          results.append(2)
          changeTo(act3)
      }

      def act3: PartialFunction[Any, Unit] = {
        case 3 =>
          results.append(3)
          changeTo(act1)
      }
    }

    fsactor start

    fsactor ! 3
    fsactor ! 2
    fsactor ! 1
    fsactor ! 1
    fsactor ! 4
    fsactor ! 2

    Thread.sleep(10)

    Assert.assertArrayEquals(Array(1, 2, 3, 1, 2), results.toArray)

    fsactor stop
  }

  @Test
  def test2b() {
    val results = Buffer[Int]()

    val fsactor = new FSActor {
      react {
        def act: PartialFunction[Any, Unit] = {
          case 1 =>
            results.append(1)
            react {
              case 2 =>
                results.append(2)
                react {
                  case 3 =>
                    results.append(3)
                    react(act)
                }
            }
        }

        act
      }
    }

    fsactor start

    fsactor ! 3
    fsactor ! 2
    fsactor ! 1
    fsactor ! 1
    fsactor ! 4
    fsactor ! 2

    Thread.sleep(10)

    Assert.assertArrayEquals(Array(1, 2, 3, 1, 2), results.toArray)

    fsactor stop
  }

  @Test
  def test3() {
    val actor = new Actor {
      def receive: PartialFunction[Any, Unit] = {
        case 'one =>
          println("actor1 receive 'one and reply 1.0")
          reply(1.0)
        case 'two =>
          println("actor1 receive 'two and reply 2.0")
          reply(2.0)
      }
    }
    actor start

    val fsactor = new FSActor {
      currentAct = act1

      var sender1: Option[Actor] = None

      def act1: PartialFunction[Any, Unit] = {
        case (sender: Option[Actor], "one") =>
          println("fsactor1 receive one")
          sender1 = sender
          println("fsactor1 send 'one to actar1")
          actor ! 'one
          println("change to fsactar2")
          this.changeTo(act2)
        case (sender: Option[Actor], "two") =>
          println("fsactor1 receive two")
          sender1 = sender
          println("fsactor1 send 'two to actar1")
          actor ! 'two
          println("change to fsactar2")
          this.changeTo(act2)
      }

      def act2: PartialFunction[Any, Unit] = {
        case n: Double =>
          println("fsactor2 receive " + n)
          println("fsactor2 send " + n.toInt + " to " + sender1.get)
          sender1.map(_ ! n.toInt)
          println("change to fsactar1")
          this.changeTo(act1)
      }
    }
    fsactor start

    val results = Buffer[Int]()
    val actor2 = new Actor {
      id = "actor2"
      override def init {
        println("actor2 send one to fsactor")
        fsactor ! (self, "one")
        println("actor2 send two to fsactor")
        fsactor ! (self, "two")
      }

      def receive: PartialFunction[Any, Unit] = {
        case x: Int =>
          println("actor2 receive " + x)
          results.append(x)
      }
    }
    actor2 start

    Thread.sleep(10)

    actor stop

    actor2 stop

    fsactor stop

    Assert.assertArrayEquals(Array(1, 2), results.toArray)
  }
}