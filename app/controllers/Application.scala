package controllers

import java.util.concurrent.TimeoutException

import model._
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson.BSONDocument


import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import play.api._
import play.api.mvc._
import play.api.mvc.Result
import play.api.libs.json.{JsArray, Json, __}
import play.api.libs.ws._

import scala.concurrent.{Await, Future}
import play.api.Play.current
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.xml.NodeSeq
import play.api.Play.current
import play.api.i18n.Messages.Implicits._

class Application extends Controller {

  val searchForm = Form (
    mapping(
      "word" ->text
    )(Define.apply)(Define.unapply)
  )

  def index = Action {
    Ok(views.html.index(searchForm))
  }

//  def result = Action {
//    Ok(views.html.result("Result"))
//  }
// def call = Action {
//
//   val url = "http://services.aonaware.com/DictService/DictService.asmx/Define?word=test"
//   def futureResult: Future[scala.xml.NodeSeq] = WS.url(url).get().map {
//     response =>
//       (response.xml \ "message")
////       (response.json \ "<WordDefinition>" \ "<Word>").as[String]
//   }
//
//   val response = futureResult
//
//   val res = Await.result(futureResult, 10 seconds)
//   var head = "Empty"
//   head = res.toString()
//   if(res.isEmpty) head = "emptyyyy" else head = "not empty"
//   response.onComplete({
//     case Success(result) =>
//       head = "Successss"
//       if(result.isEmpty) head ="success empty" else head = "success not empty"
//       //getResponse(result)
//     case Failure(exception) =>
//       head = "Failure"
//
//   })
//
//   Ok(views.html.result(head))
// }

  def call(word: String) = Action {
    def get(url: String) = scala.io.Source.fromURL(url).mkString
    val response = get("http://services.aonaware.com/DictService/DictService.asmx/Define?word="+word)
    val toXML = scala.xml.XML.loadString(response)
    Ok(views.html.result.render(formatResponse(toXML)))
  }

  //response helper function
  def formatResponse(toXML: scala.xml.NodeSeq): WordDefinition ={
    val serviceResponse = toXML
    val Word = (serviceResponse \ "Word").text
    val definitions = serviceResponse \ "Definitions"
    val definition = definitions \ "Definition"
    val defsList = new ListBuffer[Definition]

    definition.foreach{i =>

      val id= (i \ "Dictionary" \ "Id").text.toString
      val name= (i \ "Dictionary" \ "Name").text.toString
      val dicti = new Dictionary(id,name)
      val WD = (i \ "WordDefinition").text.toString
      val defin = new Definition(Word, dicti, WD)
      defsList+=defin
    }

    val count = defsList.size.toString
    val wordDefinition = new WordDefinition(Word,defsList.toList)

//    val definitions : List[Definition] = WordDefinition \ "Definitions"

    return wordDefinition

  }

  def connect() {
    // gets an instance of the driver
    // (creates an actor system)
    val driver = new MongoDriver
    val connection = driver.connection(List("localhost:27017"))

    // Gets a reference to the database "plugin"
    val db = connection("define")

    // By default, you get a BSONCollection.
    val collection = db("words")
  }

  def listDocs() = Action {

    val driver = new MongoDriver
    val connection = driver.connection(List("localhost:27017"))

    // Gets a reference to the database "plugin"
    val db = connection("define")

    // By default, you get a BSONCollection.
    val collection = db("words")
    // Select only the documents which field 'firstName' equals 'Jack'
    val query = BSONDocument("user" -> "Ross")
    // select only the fields 'lastName' and '_id'
    val filter = BSONDocument(
      "user" -> 1,
      "_id" -> 1)

    /* Let's run this query then enumerate the response and print a readable
     * representation of each document in the response */
    val futureList: Future[List[BSONDocument]] =
      collection.
      find(query, filter).
      cursor[BSONDocument].
      collect[List]()

    futureList.map { list =>
      list.foreach { doc =>
        println(s"found document: ${BSONDocument pretty doc}")
      }
    }

    val wordList = new Words(futureList)


  Ok(views.html.query.render(wordList))
  }


}
