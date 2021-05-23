package rc800.decoder

import spinal.core._
import spinal.lib._

case class ReadRegisterControl() extends Bundle {
	val registers = Vec(RegisterName(), 2)
	val part = Vec(OperandPart(), 2)
}
