package model

import reactivemongo.bson.BSONArray

/**
 * Created by Ross on 12/6/15.
 */
case class SavedWords (user:String, words: List[String])
