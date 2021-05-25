package rc800.control.component

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.ShiftOperation


case class AluControl() extends Bundle {
	val operation = AluOperation()
	val condition = Condition()
	val shiftOperation = ShiftOperation()
}


