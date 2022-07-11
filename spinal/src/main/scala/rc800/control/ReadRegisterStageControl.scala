package rc800.control

import spinal.core._
import spinal.lib._

import rc800.registers.RegisterName


case class ReadRegisterStageControl() extends Bundle {
	val registers = Vec(RegisterName(), 2)
}
