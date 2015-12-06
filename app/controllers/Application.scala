package controllers

import java.util.concurrent.TimeoutException

import model._
import org.joda.time.DateTime
import play.api.data.Forms._
import play.api.data._
import play.api.libs.iteratee.Iteratee
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import views.html.helper.input



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
    var size = 0
    futureList.map { list =>
      list.foreach { doc =>
        println(s"i found a document: ${BSONDocument pretty doc}")
        size +=1
      }
    }

    val wordList = new Words(futureList)


  Ok(views.html.query.render(wordList))
  }

  object Database {

    val collection = connect()


    def connect(): BSONCollection = {

      val driver = new MongoDriver
      val connection = driver.connection(List("localhost:27017"))

      val db = connection("define")
      db.collection("words")
    }

    def findAllWords(): Future[List[BSONDocument]] = {
      val query = BSONDocument()
      val filter = BSONDocument()

      // which results in a Future[List[BSONDocument]]
      val r = Database.collection
      .find(query, filter)
      .cursor[BSONDocument]
      .collect[List]()

      return r

    }

    def findWord(ticker: String) : Future[Option[BSONDocument]] = {
      val query = BSONDocument("user" -> "Ross")

      val bson = Database.collection
        .find(query)
        .one



      return bson
    }



  }

  def findIt() = Action.async {
    val input = Database.findAllWords()
    //listBuilder(input)

    //converter(input)
      input.map(result =>

        Ok(views.html.test.render(PersonReader.read(result.head)))
      )
  }

  object PersonReader extends BSONDocumentReader[Define] {

    def read(doc: BSONDocument) =  {

      Define(doc.getAs[BSONString]("user").get.toString)

    }

  }


  //  def converter (input: Future[List[BSONDocument]]) ={
//    StoredDoc(
//      input.getAs[BSONObjectID]("_id"),
//      input.getAs[BSONString]("title").get.value,
//      input.getAs[BSONString]("content").get.value)
//  }



//  implicit object ArticleBSONReader extends BSONReader[StoredDoc] {
//    def fromBSON(document: BSONDocument) :StoredDoc = {
//      val doc = document.toTraversable
//      StoredDoc(
//        doc.getAs[BSONObjectID]("_id"),
//        doc.getAs[BSONString]("title").get.value,
//        doc.getAs[BSONString]("content").get.value)
//    }
//  }
//  def listBuilder(input: Future[List[BSONDocument]]) = {
//
//
//    val sb = new java.lang.StringBuilder
//
//    input.map { list =>
//      list.foreach { doc =>
//        println(s"found documentttt: ${BSONDocument pretty doc}")
//        val wordList = doc.getAs[List[String]]("words")
//        for(word <- wordList){
//          sb.append(word.toString())
//          appendWord(word.toString())
//        }
//        println(s"testtet:" + sb)
//      }
//    }
//
//  }
//  def appendWord(word: String): Unit ={
//
//  }
}
