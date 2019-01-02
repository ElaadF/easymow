package fr.upem.easymow

import cats.Show
import cats.syntax.show._
import fr.upem.easymow.error.{AddOutOfBound, VehiclesSameLocation}
import fr.upem.easymow.file.IO
import fr.upem.easymow.playground.{Field, Position}
import fr.upem.easymow.vehicle.Lawnmower
import org.apache.logging.log4j
import org.apache.logging.log4j.LogManager
import org.apache.logging.log4j.Level
import scala.util.{Failure, Success}

/** Main application */
object Easymow extends App {
  val logger: log4j.Logger = LogManager.getLogger(getClass.getName)
  val RESULT = Level.forName("RESULT", 450)

  implicit val positionShow: Show[Position] =
    Show.show(p => s"""(${p.x}, ${p.y}, ${p.orientation})""")

  implicit val lawnmowerShow: Show[Lawnmower] =
    Show.show(lm => s"Position : ${lm.pos.show} Instructions : ${lm.instruction}")


  IO.read(scala.io.StdIn.readLine("Select a file: ")) match {
    case Success(content) =>
      val resultFile = IO.analyzeFormat(content)
      resultFile match {
        case Left(e) => e.foreach(err => logger.error(err))
        case Right(field) =>

          val wrongVehicle: List[Lawnmower] = field.getInvalidVehicles

          logWarnVehiclesInvalid(field, wrongVehicle)

          val cleanFieldInitial: Field = field.copy(vehicles = field.vehicles diff wrongVehicle)
          val fieldComputeAndField: (List[Either[String, Lawnmower]], Field) = cleanFieldInitial.computeField
          val fieldComputeRes: List[Either[String, Lawnmower]] = fieldComputeAndField._1
          val finalField: Field = fieldComputeAndField._2

          logTraceFieldComputation(fieldComputeRes)

          logResultLawnmower(cleanFieldInitial, finalField)
      }
    case Failure(ex) => logger.error(s"Read File failed : $ex")
  }

  /** Log the result of the computation of field by compares
    * the initial field and the final field
    *
    *  @param cleanFieldInitial the initial field without invalid vehicles
    *  @param finalField the final field after all computations
    */
  def logResultLawnmower(cleanFieldInitial: Field, finalField: Field): Unit ={
    val displayVehicle = cleanFieldInitial.vehicles zip finalField.vehicles
    displayVehicle.foreach{case(v1, v2) => logger.log(RESULT, s"${v1.pos.show} => ${v2.pos.show}") }
  }

  /** Log the trace of the computation
    * the initial field and the final field
    *
    *  @param fieldComputeRes the trace of the process done
    */
  def logTraceFieldComputation(fieldComputeRes: List[Either[String, Lawnmower]]): Unit = {
    fieldComputeRes.foreach {
      case Left(impossibleInstr) => logger.warn(impossibleInstr)
      case Right(vehicleFinalState) => logger.info(vehicleFinalState.show)
    }
  }

  /** Log error of invalid vehicles
    *
    *  @param field the field were the computation will be done
    *  @param wrongVehicle the list of all vehicles who is invalid for the field
    */
  def logWarnVehiclesInvalid(field: Field, wrongVehicle: List[Lawnmower]): Unit = {
    if (wrongVehicle.nonEmpty) {
      wrongVehicle.foreach {
        case l if !field.isInField(l.pos.x, l.pos.y) => logger.warn(AddOutOfBound.errorMessage((l.pos.x, l.pos.y)))
        case l if !field.isFreeZone(l.pos.x, l.pos.y) => logger.warn(VehiclesSameLocation.errorMessage((l.pos.x, l.pos.y)))
      }
    }
  }
}

