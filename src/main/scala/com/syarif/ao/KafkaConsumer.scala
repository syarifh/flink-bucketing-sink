package com.syarif.ao

import java.time._

import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer010

import java.util.{ Properties }

import org.apache.flink.streaming.util.serialization.KeyedDeserializationSchema
import org.apache.flink.api.common.typeinfo.TypeInformation

import com.google.protobuf.util.JsonFormat
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.connectors.fs.StringWriter
import org.apache.flink.streaming.connectors.fs.bucketing._

case class Transaction(json: String, et: Long)

object JsonPrinter {
val jsonPrinter = JsonFormat.printer()
  .omittingInsignificantWhitespace()
  .preservingProtoFieldNames()
  .includingDefaultValueFields()
}

object LogDeserialization extends KeyedDeserializationSchema[Transaction] {
    override def deserialize(messageKey: Array[Byte], message: Array[Byte], topic: String, partition: Int, offset: Long): Transaction = {
      val utcZoneId = ZoneId.of("UTC")
      val zonedDateTime = ZonedDateTime.now
      val utcDateTime = zonedDateTime.withZoneSameInstant(utcZoneId)
      val format = new java.text.SimpleDateFormat("YYYY-MM-DD HH:MM[:SS[.SSSSSS]]")
      val dateTime = format.format(new java.util.Date())
      val json_template = "{\"request_id\":\"\",\"user_id\":\"\",\"user_name\":\"\",\"user_email\":\"\",\"user_phone\":\"\",\"user_locale\":\"\",\"user_type\":\"\",\"registered_device\":[],\"event_timestamp\": null,\"login_request_status\":\"\",\"device_unique_id\":\"\",\"device_app_id\":\"\",\"device_push_token\":\"\",\"device_instance_token\":\"\",\"retry_count\":0,\"device_imei\":\"\",\"device_model\":\"\",\"device_manufacturer\":\"\",\"device_fingerprint\":\"\",\"ip_address\":\"\"}"
      try {
        val msg = TransactionMessage.parseFrom(message)
        Transaction(JsonPrinter.jsonPrinter.print(msg), msg.getEventTimestamp.getSeconds)

      } catch {
        case e: Exception => Transaction(json_template, utcDateTime.getSecond)
      }
    }
    val format = new java.text.SimpleDateFormat("yyMMddHHmm")
    override def isEndOfStream(p: Transaction): Boolean
    = (false)

    override def getProducedType(): TypeInformation[Transaction]
    = createTypeInformation[Transaction]

}

object Consumer {
  private val props = new Properties

  props.setProperty("bootstrap.servers", "127.0.0.1:<bootstrap port>")
  props.setProperty("group.id", "transcation-group-01")
  props.setProperty("client.id", "transaction-client-01")
  props.setProperty("max.poll.records", "100")
val consumer_es = new FlinkKafkaConsumer010[Transaction]("transaction-log", LogDeserialization, props)
}

object KafkaConsumer {
  def main(args: Array[String]): Unit = {
    while (true) {
      val env = StreamExecutionEnvironment.getExecutionEnvironment
      env.setParallelism(1)
      env.setMaxParallelism(2)
      env.enableCheckpointing(1000).getCheckpointConfig
      val source_es = Consumer.consumer_es

      val counts: DataStream[Transaction] = env.addSource(source_es)
      val counts_final = counts.map { p => p.json }
      val sink = new BucketingSink[(String)]("/tmp/output/transaction-log/")
      sink.setBucketer(new DateTimeBucketer[(String)]("yyyy-MM-dd--HHmm"))
      sink.setWriter(new StringWriter())
      sink.setBatchSize(1024 * 1024 * 4) // this is 4 MB,

      counts_final.addSink(sink)
//      counts_final.print()

      env.execute()
    }
  }
}
