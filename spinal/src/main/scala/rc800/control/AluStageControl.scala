package rc800.control

import spinal.core._

import rc800.alu.OperandSource

import component.AluControl
import component.PcControl


case class AluStageControl() extends Bundle {
    val selection = Vec(OperandSource(), 2)
	val pcControl = PcControl()
    val aluControl = AluControl()
}
