package rc800.decoder

import spinal.core._
import spinal.lib._

import rc800.registers.Register
import rc800.registers.OperandPart

case class ReadRegisterControl() extends Bundle {
	val registers = Vec(Register(), 2)
	val part = Vec(OperandPart(), 2)
}
