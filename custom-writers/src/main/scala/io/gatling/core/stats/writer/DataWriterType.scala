package io.gatling.core.stats.writer

object DataWriterType {

  private val AllTypes = Seq(
    //Our new AwesomeWriterType
    AwesomeWriterType,
    //Attach the default types from Gatling
    ConsoleDataWriterType,
    FileDataWriterType,
    GraphiteDataWriterType,
    LeakReporterDataWriterType)
    .map(t => t.name -> t).toMap

  def findByName(name: String): Option[DataWriterType] = AllTypes.get(name)
}

abstract class DataWriterType(val name: String, val className: String)

// Define a new DataWriterType for our AwesomeWriter
object AwesomeWriterType extends DataWriterType(AwesomeWriter.KEY, AwesomeWriter.IMPL)