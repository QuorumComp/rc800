package rc800.decoder

import spinal.core._
import spinal.lib._

import rc800.registers.OperandPart
import rc800.registers.RegisterName

case class ReadRegisterControl() extends Bundle {
	val registers = Vec(RegisterName(), 2)
	val part = Vec(OperandPart(), 2)
}
