package model

import reactivemongo.bson.BSONDocument

import scala.concurrent.Future

/**
 * Created by Ross on 11/30/15.
 */
case class Words (Words: Future[List[BSONDocument]])