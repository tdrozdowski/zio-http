package zhttp.http

import io.netty.buffer.Unpooled
import io.netty.handler.codec.base64.Base64
import io.netty.handler.codec.http.{DefaultHttpHeaders, HttpHeaderNames, HttpHeaderValues, HttpHeaders}
import io.netty.util.CharsetUtil
import zhttp.http.HeadersHelpers.BasicSchemeName

import scala.jdk.CollectionConverters._
import scala.util.{Failure, Success, Try}

final case class Header private[Header] (name: CharSequence, value: CharSequence)

object Header {

  /**
   * Converts a List[Header] to [io.netty.handler.codec.http.HttpHeaders]
   */
  def disassemble(headers: List[Header]): HttpHeaders =
    headers.foldLeft[HttpHeaders](new DefaultHttpHeaders()) { case (headers, entry) =>
      headers.set(entry.name, entry.value)
    }

  def make(headers: HttpHeaders): List[Header] =
    headers
      .iteratorCharSequence()
      .asScala
      .map(h => Header(h.getKey, h.getValue))
      .toList

  // Helper utils to create Header instances
  val acceptJson: Header     = Header(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_JSON)
  val acceptXhtmlXml: Header = Header(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_XHTML)
  val acceptXml: Header      = Header(HttpHeaderNames.ACCEPT, HttpHeaderValues.APPLICATION_XML)
  val acceptAll: Header      = Header(HttpHeaderNames.ACCEPT, "*/*")

  val contentTypeJson: Header           = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_JSON)
  val contentTypeXml: Header            = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_XML)
  val contentTypeXhtmlXml: Header       = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_XHTML)
  val contentTypeTextPlain: Header      = Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.TEXT_PLAIN)
  val transferEncodingChunked: Header   = Header(HttpHeaderNames.TRANSFER_ENCODING, HttpHeaderValues.CHUNKED)
  def contentLength(size: Long): Header = Header(HttpHeaderNames.CONTENT_LENGTH, size.toString)
  val contentTypeFormUrlEncoded: Header =
    Header(HttpHeaderNames.CONTENT_TYPE, HttpHeaderValues.APPLICATION_X_WWW_FORM_URLENCODED)

  def host(name: String): Header           = Header(HttpHeaderNames.HOST, name)
  def userAgent(name: String): Header      = Header(HttpHeaderNames.USER_AGENT, name)
  def location(value: String): Header      = Header(HttpHeaderNames.LOCATION, value)
  def authorization(value: String): Header = Header(HttpHeaderNames.AUTHORIZATION, value)

  def cookieParser[M <: Meta](cookie: List[Option[(String, String)]]): String =
    Try {
      cookie.map {
        case Some(p) => p._1 + "=" + p._2
        case None    => throw new Exception()
      }
    } match {
      case Failure(_)     => "Invalid cookie: cannot use Separators or control characters"
      case Success(value) => value.mkString("; ")
    }

  def cookies[M <: Meta](cookie: List[Option[Cookie[M]]]): Header = {
    Header(
      HttpHeaderNames.COOKIE,
      cookieParser(
        cookie.map {
          case Some(value) =>
            Cookie.validateCookie(value.name) match {
              case Some(_) => None
              case None    => Some((value.name, value.content))
            }

          case None => None
        },
      ),
    )
  }

  def cookies(response: UHttpResponse): Header = cookies(
    response.cookies,
  )

  def cookie(cookie: Cookie[Nothing]): Header = Header(HttpHeaderNames.COOKIE, cookie.name + "=" + cookie.content)

  def setCookie[M <: Meta](cookie: Cookie[M]): Header =
    Header(HttpHeaderNames.SET_COOKIE, cookie.toString)

  def setCookieString(cookie: String): Header =
    Header(HttpHeaderNames.SET_COOKIE, cookie)

  def removeCookie[M <: Meta](cookie: String): Header =
    Header(HttpHeaderNames.SET_COOKIE, Cookie[M](cookie, "", None).toString) //TODO

  def basicHttpAuthorization(username: String, password: String): Header = {
    val authString    = String.format("%s:%s", username, password)
    val authCB        = Unpooled.wrappedBuffer(authString.getBytes(CharsetUtil.UTF_8))
    val encodedAuthCB = Base64.encode(authCB)
    val value         = String.format("%s %s", BasicSchemeName, encodedAuthCB.toString(CharsetUtil.UTF_8))
    Header(HttpHeaderNames.AUTHORIZATION, value)
  }

  def createAuthorizationHeader(value: String): Header = Header(HttpHeaderNames.AUTHORIZATION, value)

  /**
   * Use built-in header methods for better performance.
   */
  def custom(name: String, value: CharSequence): Header = Header(name, value)

  def parse(headers: HttpHeaders): List[Header] =
    headers.entries().asScala.toList.map(entry => Header(entry.getKey, entry.getValue))

}
