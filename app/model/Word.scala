package model

import play.api.libs.json.Json

/**
 * Created by Ross on 11/30/15.
 */
case class Word (name: String, words: List[String])

object JsonFormats{
  implicit val wordFormat = Json.format[Word]
}
