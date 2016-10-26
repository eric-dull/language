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
object Language {

  val src = "./resources/classifiers/"
  val classifier1 = "english.all.3class.distsim.crf.ser.gz"
  val validEntities = Array("PERSON", "ORGANIZATION","LOCATION")
  val text: String = scala.io.Source.fromFile("resources/text.txt", "utf-8").getLines.mkString
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


  /********************
    *
    * BELOW :
    *
    *******************
    * UNUSED SANDBOX
    *
    *
    *
    *******************/


  def testWithClassifier(): Unit = {

    val lines: String = scala.io.Source.fromFile("resources/text.txt.bak", "utf-8").getLines.mkString
    val serializedClassifier = src + classifier1

    val classifier = CRFClassifier.getClassifier(serializedClassifier)
    val out = classifier.classify(lines)//.asInstanceOf[util.List[util.List[String]]]

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
            print("[/" + oldWordClass + "]")
          }
        }

        if (!wordClass.equals("O") && !wordClass.equals("")) {
          if (!oldWordClass.equals(wordClass)) {
            print("[" + wordClass + "]")
          }
        }

        oldWordClass = wordClass

        words = words + 1
        print(word)
        print(" ")

        if (words > 10) {
          words = 0
          println(" ")
        }
      }
    }

  }


  def test(): Unit = {

    val tg = new TaggerConfig()
    tg.setProperties(props)


    val sent: Sentence = new Sentence("Lucy is in the sky with diamonds.")
    val nerTags = sent.nerTags(tg)  // [PERSON, O, O, O, O, O, O, O]
    val firstPOSTag = sent.posTag(0)
    println(firstPOSTag)

  }

  def testWithPipeline(): Unit = {

    val props = new Properties()
    props.setProperty("annotators", "tokenize, ssplit, pos, ner")
    props.setProperty("pos.model", src + classifier1)
    props.setProperty("ner.model", src + classifier1)
    //    props.setProperty("parse.model", src + classifier1)
    props.setProperty("ner.useSUTime", "false")

    val pipeline: StanfordCoreNLP = new StanfordCoreNLP(props)


    // TODO : use pipeline jsonPrint !
    val document: Annotation = new Annotation(text)

    pipeline.annotate(document)
    val sentences = document.get(classOf[SentencesAnnotation])

    println(sentences)
  }


  // MAIN
  //  def main(args : Array[String]): Unit = {
  //    //Language.getEntitiesFromText()
  //    println(getEntitiesByType(text))
  //  }

  // Extract Entities from a text
  /*
    def getEntitiesFromText(): Unit = {

      val entitySets: ArrayBuffer[ArrayBuffer[Entity]] = ArrayBuffer()

      /*** We make a custom text split over points and over commas
       to treat each sentence part separately **/

      // 1 - We iterate over sentences parts, splitted on dots and commas
      for (sentence <- text.split('.')) {
        for (part <- sentence.split(',')) {

          // 2 - We call the classifier on the sentence part
          val labelSets = classifier.classify(part)

          // 3 - For each set of labels, we call our getEntitiesFromLabel method
          for (i <- 0 to labelSets.size - 1) {
            val labelSet = labelSets.get(i).map { label: CoreLabel =>
              label
            }.asInstanceOf[ArrayBuffer[CoreLabel]]
            entitySets += getEntitiesFromLabels(labelSet, ArrayBuffer(), "")
          }

  //        val labelSets = classifier.classify(part).asInstanceOf[Array[Array[CoreLabel]]]
  //        labelSets.map { set: Array[CoreLabel] =>
  //          // val labelSet = set.asInstanceOf[ArrayBuffer[CoreLabel]]
  //          val labelSet = set.map { label: CoreLabel =>
  //            label
  //          }.asInstanceOf[ArrayBuffer[CoreLabel]]
  //          entitySets += getEntitiesFromLabels(labelSet, ArrayBuffer(), "")
  //        }.map(_.toArray)
        }
      }

      for (set <- entitySets) {
        for (e <- set) {
          println(e.name + " " + e.`type`)
        }
      }
    }

  // Custom Entities extraction from Stanford Parser CoreLabel objects
  // Recursive function using single labels and entities lists
    def getEntitiesFromLabels(labels: ArrayBuffer[CoreLabel], entities: ArrayBuffer[Entity], lastLabelType: String): ArrayBuffer[Entity] = {

      // if tags is empty, we return the entities
      if(labels.isEmpty) {
        entities.map {e => e.name = e.nameParts.mkString(" ") }
        return entities
      }

      val label = labels.remove(0) // pop tag and get its content
      val word = label.word()
      val labelType = label.get(classOf[CoreAnnotations.AnswerAnnotation])


      if (validEntities.contains(labelType)) { // if the current tag is a Person or an Organization
        // check if we need to add an entity to the entities array
        if (entities.size == 0 || !validEntities.contains(lastLabelType) || entities.last.`type`.ne(labelType)) {
          entities += new Entity(labelType, ArrayBuffer(), "")
        }

        // append tag word to the name parts of the last entity from the entities list
        entities.last.nameParts += word

      }

      // recursive call on get_entities_from_tags until there's no tag left
      getEntitiesFromLabels(labels, entities, labelType)
    }*/



}




// Create a document. No computation is done yet.
//    val doc = new Document(text)
//    val sentences: util.List[Sentence] = doc.sentences()
//    println(sentences.get(0))
//  for (sent: Sentence <- sentences) {  // Will iterate over two sentences
//      println(sent.nerTags())
// We're only asking for words -- no need to load any models yet
//      System.out.println("The second word of the sentence '" + sent + "' is " + sent.word(1))
//      // When we ask for the lemma, it will load and run the part of speech tagger
//      System.out.println("The third lemma of the sentence '" + sent + "' is " + sent.lemma(2))
//      // When we ask for the parse, it will load and run the parser
//      System.out.println("The parse of the sentence '" + sent + "' is " + sent.parse())
// ...
//}