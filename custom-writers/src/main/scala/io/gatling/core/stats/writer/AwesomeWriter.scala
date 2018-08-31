package io.gatling.core.stats.writer

import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}

import scala.collection.concurrent.TrieMap
import scala.collection.mutable.ListBuffer


/**
  * DataWriterData for AwesomeWriterData.
  * The sample AwesomeWriter does not need configuration values so far.
  */
case class AwesomeWriterData() extends DataWriterData

/**
  * Custom DataWriter implementation to provide Gatling results as JSON output
  */
class AwesomeWriter extends DataWriter[AwesomeWriterData] {

  /**
    * Custom id type to represent a group in the group hierarchy
    */
  type HierarchyId = (Long, String)

  /**
    * Internal buffer to store JSON Objects for requests and for groups as well
    */
  private val nodeBuffer: TrieMap[HierarchyId, ListBuffer[ObjectNode]] = TrieMap[HierarchyId, ListBuffer[ObjectNode]]()

  /**
    * Jackson Mapper to create ObjectNode
    */
  private val JSON_MAPPER = new ObjectMapper()


  //----------------------------------
  // Overrides: DataWriter
  //----------------------------------

  override def onInit(init: Init): AwesomeWriterData = {
    AwesomeWriterData()
  }

  override def onMessage(message: LoadEventMessage,
                         data: AwesomeWriterData): Unit =
    message match {
      case m: GroupMessage => handleGroup(m, data)
      case m: ResponseMessage => handleResponse(m, data)
      case _ =>
    }

  //-- Unused methods for this writer
  override def onFlush(data: AwesomeWriterData): Unit = {}

  override def onCrash(cause: String,
                       data: AwesomeWriterData): Unit = {}

  override def onStop(data: AwesomeWriterData): Unit = {}

  //----------------------------------
  // Internal Methods
  //----------------------------------

  /**
    * Creates a new ObjectNode representing a GroupMessage.
    * All collected request objects will be attached.
    * If this is the top level group - results are written.
    *
    * @param groupMessage The GroupMessage to be processed
    * @param data         The AwesomeWriterData
    */
  def handleGroup(groupMessage: GroupMessage,
                  data: AwesomeWriterData): Unit = {

    // Create the group node
    val groupNode = JSON_MAPPER.createObjectNode()
      .put("type", "group")
      .put("scenario", groupMessage.scenario)
      .put("duration", groupMessage.duration)
      .put("timestamp", groupMessage.startTimestamp)
      .put("name", groupMessage.groupHierarchy.last)
      .put("responseTime", groupMessage.cumulatedResponseTime)

    // Attach children
    val children = nodeBuffer.get(getHierarchyId(groupMessage.userId, groupMessage.groupHierarchy))
    if (children.isDefined) {
      val arr = JSON_MAPPER.createArrayNode()
      children.get.foreach(o => arr.add(o))
      groupNode.set("children", arr)
    }

    if (isTopLevelGroup(groupMessage)) {
      transmit(groupNode, data)
    } else {
      updateBuffer(getHierarchyId(groupMessage.userId, groupMessage.groupHierarchy, parentHierarchyId = true), groupNode)
    }
  }

  /**
    * Transforms a ResponseMessage to an ObjectNode and puts it to the buffer
    *
    * @param responseMessage The ResponseMessage to be processed
    * @param data            The AwesomeWriterData
    */
  def handleResponse(responseMessage: ResponseMessage,
                     data: AwesomeWriterData): Unit = {

    val responseNode = JSON_MAPPER.createObjectNode()
      .put("request", responseMessage.name)
      .put("timestamp", responseMessage.timings.startTimestamp)
      .put("status", responseMessage.status.name)
      .put("responseCode", responseMessage.responseCode.getOrElse("-1").toInt)
      .put("responseTime", responseMessage.timings.responseTime)
      .put("message", responseMessage.message.getOrElse(""))

    responseMessage.extraInfo.foreach(info => {
      val strings = info.asInstanceOf[String].split("__")
      responseNode.put(strings(0), strings(1))
    })

    updateBuffer(getHierarchyId(responseMessage.userId, responseMessage.groupHierarchy), responseNode)
  }

  /**
    * Checks if we are currently dealing with a top level group.
    * If groupHierarchy is 1 it is the top level hierarchy
    *
    * @param groupMessage The GroupMessage to be checked
    * @return True if top level, false otherwise
    */
  private def isTopLevelGroup(groupMessage: GroupMessage) = groupMessage.groupHierarchy.length == 1

  /**
    * Creates an id for a certain user and the current group we are dealing with.
    * If the current hierarchy is e.g. groupA,groupB and user has id 1 the id would be:
    * HierarchyId = (1,groupA-groupB)
    *
    * @param userId            The id of the current user
    * @param hierarchy         The current group hierarchy
    * @param parentHierarchyId Flag to indicate that the id for the parent group should be created
    * @return A new HierarchyId
    */
  private def getHierarchyId(userId: Long,
                             hierarchy: List[String],
                             parentHierarchyId: Boolean = false): HierarchyId = {

    new HierarchyId(userId, (if (parentHierarchyId) hierarchy.dropRight(1) else hierarchy).mkString("-"))
  }


  /**
    * Attach a new ObjectNode the the buffer
    *
    * @param key      The HierarchyId
    * @param jsonNode The ObjectNode to be attached
    * @return
    */
  private def updateBuffer(key: HierarchyId,
                           jsonNode: ObjectNode): ListBuffer[ObjectNode] = {

    nodeBuffer.getOrElseUpdate(key, new ListBuffer[ObjectNode]) += jsonNode
  }

  /**
    * Finally transmits the results.
    * In our awesome implementation, we dump it to the console
    *
    * @param node The JSON Object to be written
    * @param data The AwesomeWriterData to fetch possible configs
    */
  private def transmit(node: JsonNode,
                       data: AwesomeWriterData): Unit = {

    println(s"My Awesome writer says: \n ${JSON_MAPPER.writerWithDefaultPrettyPrinter().writeValueAsString(node)}")
  }


}

/**
  * Companion Object for the AwesomeWriter
  */
object AwesomeWriter {

  /**
    * The key of the Writer. This used to register the writer in io.gatling.core.stats.writer.DataWriterType
    */
  val KEY: String = "awesome"

  /**
    * The full qualified name of the AwesomeWriter used to register the writer in io.gatling.core.stats.writer.DataWriterType
    */
  val IMPL: String = classOf[AwesomeWriter].getCanonicalName

}