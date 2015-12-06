package controllers



import model._
import play.api.data.Forms._
import play.api.data._
import reactivemongo.api.{MongoConnection, MongoDriver}
import reactivemongo.api.collections.bson.BSONCollection
import reactivemongo.bson._
import scala.collection.mutable.ListBuffer
import scala.util.{Failure, Success, Try}
import play.api.mvc._
import play.api.libs.json.{JsArray, Json, __}
import scala.concurrent.{Await, Future}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
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

    def addWord(word: String) ={
      val selector = BSONDocument("user" -> "Ross")
      val modifier = BSONDocument(
        "$push" -> BSONDocument(
        "words" -> word
        ))
      collection.update(selector, modifier)
    }


  }

  def findIt() = Action.async {
    val input = Database.findAllWords()
    //listBuilder(input)

    //converter(input)
      input.map(result =>

        Ok(views.html.db.render(PersonReader.read(result.head)))
      )
  }
  def add(word: String)= Action.async {

    val futureAdd = Database.addWord(word)
    Await.result(futureAdd,10 seconds)
    val input = Database.findAllWords()
    //listBuilder(input)

    //converter(input)
    input.map(result =>

      Ok(views.html.db.render(PersonReader.read(result.head)))
    )
  }

  object PersonReader extends BSONDocumentReader[SavedWords] {

    def read(doc: BSONDocument) =  {

      SavedWords(doc.getAs[String]("user").get.toString, doc.getAs[List[String]]("words").toList.flatten)

    }

  }

}
