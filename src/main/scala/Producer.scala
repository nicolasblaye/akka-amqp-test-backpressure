import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings, Supervision}
import akka.stream.alpakka.amqp.scaladsl.{AmqpSink, AmqpSource}
import akka.stream.alpakka.amqp.{AmqpConnectionFactoryConnectionProvider, AmqpWriteSettings, NamedQueueSourceSettings, WriteMessage}
import akka.stream.scaladsl.{Keep, RestartSource, Sink, Source}
import akka.util.ByteString
import com.rabbitmq.client.ConnectionFactory
import com.typesafe.config.ConfigFactory

import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global

object Common {

  val decider: Supervision.Decider = { e =>
    println(s"Error in the stream $e")
    Supervision.Restart
  }

  private val config = ConfigFactory.load
  implicit val system = ActorSystem("test", config)
  implicit val mat = ActorMaterializer(ActorMaterializerSettings(config.getConfig("akka.stream.materializer")).withSupervisionStrategy(decider)
    .withDebugLogging(true))
}

object Producer extends App {
  import Common._

  val factory: ConnectionFactory = {
    val f = new ConnectionFactory
    f.setHost("rabbit-server")
    f.setUsername("admin")
    f.setPassword("admin")
    f.setAutomaticRecoveryEnabled(false)
    f.setTopologyRecoveryEnabled(false)
    f
  }

  val events: Iterator[Int] = (1 until 10000).toIterator

  val writeSettings: AmqpWriteSettings = AmqpWriteSettings(AmqpConnectionFactoryConnectionProvider(factory))
    .withExchange("test")


  Source
    .fromIterator(() => events)
    .map(i => WriteMessage(ByteString(i)))
    .runWith(AmqpSink(writeSettings))
}

object SlowConsumer extends App {
  import Common._
  import scala.concurrent.duration._

  val factory: ConnectionFactory = {
    val f = new ConnectionFactory
    f.setHost("localhost")
    f.setUsername("admin")
    f.setPassword("admin")
    f.setAutomaticRecoveryEnabled(false)
    f.setTopologyRecoveryEnabled(false)
    f
  }
  val queueSettings = NamedQueueSourceSettings(AmqpConnectionFactoryConnectionProvider(factory), "test")
      .withAckRequired(false)

  /**
    * I use a restart source because otherwise the graph fails silently, bypassing the decider
    */
  RestartSource.withBackoff(minBackoff = 1 second, maxBackoff = 30 seconds, randomFactor = 0.2, maxRestarts = 1)(
    () => AmqpSource.committableSource(queueSettings, 10))
    .map(_.message.bytes)
    .mapAsync(10) {bytes => Future { Thread.sleep(1000); println(s"Processed message ${bytes}"); bytes }}
    .runWith(Sink.foreach(bytes => println(bytes)))
}
