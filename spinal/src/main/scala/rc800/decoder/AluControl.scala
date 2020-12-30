package rc800.decoder

import spinal.core._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSelection
import rc800.alu.ShiftOperation

case class AluControl() extends Bundle {
    val selection = Vec(OperandSelection(), 2)
    
	val operation = AluOperation()
	val condition = Condition()

	val shiftOperation = ShiftOperation()
}


