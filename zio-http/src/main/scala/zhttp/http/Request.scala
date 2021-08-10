package zhttp.http

import io.netty.handler.codec.http.{HttpHeaderNames, HttpUtil}
import zhttp.core.JChannelHandlerContext

import java.net.{InetAddress, InetSocketAddress}

// REQUEST
final case class Request(
  endpoint: Endpoint,
  headers: List[Header] = List.empty,
  content: HttpData[Any, Nothing] = HttpData.empty,
  private val channelContext: JChannelHandlerContext = null,
) extends HasHeaders
    with HeadersHelpers { self =>
  val method: Method = endpoint._1
  val url: URL       = endpoint._2
  val route: Route   = method -> url.path

  val charset = getHeaderValue(HttpHeaderNames.CONTENT_TYPE) match {
    case Some(value) => HttpUtil.getCharset(value, HTTP_CHARSET)
    case None        => HTTP_CHARSET
  }

  def getBodyAsString: Option[String] = content match {
    case HttpData.CompleteData(data) => Option(new String(data.toArray, charset))
    case _                           => Option.empty
  }

  def remoteAddress: Option[InetAddress] = {
    if (channelContext != null && channelContext.channel().remoteAddress().isInstanceOf[InetSocketAddress])
      Some(channelContext.channel().remoteAddress().asInstanceOf[InetSocketAddress].getAddress)
    else
      None
  }

}

object Request {}
