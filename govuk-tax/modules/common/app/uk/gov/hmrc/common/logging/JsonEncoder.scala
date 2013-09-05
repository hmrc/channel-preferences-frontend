package uk.gov.hmrc.common.logging

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.core.JsonGenerator.Feature
import org.apache.commons.lang.time.FastDateFormat
import ch.qos.logback.core.encoder.EncoderBase
import ch.qos.logback.classic.spi.{ ThrowableProxyUtil, ILoggingEvent }
import org.apache.commons.io.IOUtils._

class JsonEncoder extends EncoderBase[ILoggingEvent] {

  import collection.JavaConversions._

  private val mapper = new ObjectMapper().configure(Feature.ESCAPE_NON_ASCII, true)

  private val dateFormat = FastDateFormat.getInstance("yyyy-MM-dd HH:mm:ss.SSSZZ")

  override def doEncode(event: ILoggingEvent) {
    val eventNode = mapper.createObjectNode

    eventNode.put("app", "govuk-tax")
    eventNode.put("timestamp", dateFormat.format(event.getTimeStamp))
    eventNode.put("message", event.getFormattedMessage)

    Option(event.getThrowableProxy).map(p =>
      eventNode.put("exception", ThrowableProxyUtil.asString(p))
    )

    eventNode.put("logger", event.getLoggerName)
    eventNode.put("thread", event.getThreadName)
    eventNode.put("level", event.getLevel.toString)

    Option(getContext).map(c =>
      c.getCopyOfPropertyMap.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }
    )
    event.getMDCPropertyMap.toMap foreach { case (k, v) => eventNode.put(k.toLowerCase, v) }

    write(mapper.writeValueAsBytes(eventNode), outputStream)
    write(LINE_SEPARATOR, outputStream)

    outputStream.flush
  }

  override def close() {
    write(LINE_SEPARATOR, outputStream)
  }
}

