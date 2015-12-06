package model

import reactivemongo.bson.{BSONArray, BSONString, BSONObjectID}

/**
 * Created by Ross on 12/4/15.
 */
case class StoredDoc (_id: BSONObjectID, user: BSONString, words: BSONArray)
