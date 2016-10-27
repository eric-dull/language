/**
  * Created by nbarthedejean on 21/10/2016.
  */

package network.eic.language

import java.util
import java.util.Properties

import edu.stanford.nlp.ling.CoreAnnotations.SentencesAnnotation
import edu.stanford.nlp.pipeline.{Annotation, StanfordCoreNLP}
import edu.stanford.nlp.simple._
import edu.stanford.nlp.ie.crf.CRFClassifier
import edu.stanford.nlp.ling.{CoreLabel, CoreAnnotations}
import edu.stanford.nlp.tagger.maxent.TaggerConfig
import scala.collection.JavaConversions._
import scala.collection.immutable.Iterable
import scala.collection.mutable

import scala.collection.mutable.ArrayBuffer

//import edu.stanford.nlp.ling.Sentence
import edu.stanford.nlp.simple._
import com.google.protobuf._

import scala.io.BufferedSource

case class EntityPositionList(name: String, positions: Array[Array[Integer]])
case class SimpleEntity(name: String, `type`: String, position: Array[Integer])
case class Entity(`type`: String, nameParts: ArrayBuffer[String], position: Array[Integer])

class EntityProcessor {

  val src = "./resources/classifiers/"
  val classifier1 = "english.all.3class.distsim.crf.ser.gz"
  val validEntities = Array("PERSON", "ORGANIZATION","LOCATION")
  val serializedClassifier = src + classifier1
  val classifier = CRFClassifier.getClassifier(serializedClassifier)



  val props = new Properties()
  props.setProperty("annotators", "tokenize, ssplit, pos, ner")
  props.setProperty("pos.model", src + classifier1)
  props.setProperty("ner.model", src + classifier1)
  // props.setProperty("parse.model", src + classifier1)
  props.setProperty("ner.useSUTime", "false")


  // Inspired by this work : https://www.garysieling.com/blog/entity-recognition-with-scala-and-stanford-nlp-named-entity-recognizer
  def getEntities(text: String): Array[SimpleEntity] = {

    val out = classifier.classify(text)//.asInstanceOf[util.List[util.List[String]]]

    var words = 0
    var entitiesBuffer = ArrayBuffer[Entity]()

    for (i <- 0 to out.size() - 1) {

      val sentence: util.List[CoreLabel] = out.get(i)
      var oldWordClass: String = ""

      for (j <- 0 to sentence.size() - 1) {
        val word: CoreLabel = sentence.get(j)
        val wordClass: String = word.get(classOf[CoreAnnotations.AnswerAnnotation])

        // If the word has a valid type,
        // we check if it's a new entity, or if it follows one of the same type
        if (validEntities.contains(wordClass)) {

          // Create a new entity
          if (!oldWordClass.equals(wordClass)) entitiesBuffer += new Entity(
            `type` = wordClass,
            nameParts = ArrayBuffer(word.word()),
            position = Array(word.beginPosition(), word.endPosition())
          )

          // Or Complete the object for the last entity in the ArrayBuffer
          else  {
            entitiesBuffer.last.nameParts += word.word()
            entitiesBuffer.last.position(1) = word.endPosition() // update the "end pos"
          }
        }

        oldWordClass = wordClass
        words = words + 1
      }
    }

    // We make the Array Buffer an Array
    // We call "distinct" to get unique values on it
    // We build a clean "SimpleEntity" instance from it
    entitiesBuffer.distinct.toArray.map{ e =>
      new SimpleEntity(e.nameParts.mkString(" "), e.`type`, e.position)
    }
  }

  def getEntityPositionsByType(text: String): Map[String, Map[String, Array[Array[Integer]]]]  = {

    // 1- We group entities by type (Person, Organization, Location)
    getEntities(text).groupBy(_.`type`).
      // 2- In the Entity by Type map, we go through the values
    mapValues { singleTypeEntities =>
        // 3- and aggregate the positions of each entity by its name
      singleTypeEntities.groupBy(_.name).mapValues( _.map(_.position))
    }

  }

  def getEntitiesByType(text:String) = getEntities(text).groupBy(_.`type`).mapValues(_.map(_.name))

  def getHighlightedText(text: String): String = {

    val classByEntityType = Map(
      "ORGANIZATION" -> "#547BD6",
      "PERSON" -> "#D64757",
      "LOCATION" -> "#6ADE74"
    )

    val classifier = CRFClassifier.getClassifier(serializedClassifier)
    val out = classifier.classify(text)
    var outputText = "<html><head><meta charset='utf-8'></head><body>"
    var words = 0
    for (i <- 0 to out.size() - 1) {
      val sentence = out.get(i)

      var foundWord = ""
      var oldWordClass = ""

      for (j <- 0 to sentence.size() - 1) {
        val word = sentence.get(j)
        val wordClass = word.get(classOf[CoreAnnotations.AnswerAnnotation]) + ""

        if (!oldWordClass.equals(wordClass)) {
          if (!oldWordClass.equals("O") && !oldWordClass.equals("")) {
            outputText += "</span></a>"
          }
        }

        if (!wordClass.equals("O") && !wordClass.equals("")) {
          if (!oldWordClass.equals(wordClass)) {
            outputText +=
            "<a href='#' onclick=\"parent.location='http://localhost:8000/?collections=snoop&q=" + word + "&size=10&order=Relevance'\">" +
            "<span " +
              "class='entity-" + wordClass.toLowerCase + "' " +
              "data-entity='" + word + "' " +
              "style='color:white;background-color:" + classByEntityType.get(wordClass).get +  "'>"
          }
        }

        oldWordClass = wordClass
        words = words + 1
        outputText += word
        outputText += " "
      }
    }

    outputText + "</body></html>"
  }



}
