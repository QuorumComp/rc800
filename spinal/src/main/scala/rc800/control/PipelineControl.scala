package rc800.control

import spinal.core._
import spinal.lib._

import rc800.alu.AluOperation
import rc800.alu.OperandSource
import rc800.alu.PcTruePathSource

import rc800.registers.OperandPart
import rc800.registers.RegisterName
import rc800.registers.WriteMask


case class PipelineControl() extends Bundle {
	val readStageControl   = ReadRegisterStageControl()
	val memoryStageControl = MemoryStageControl()
	val aluStageControl    = AluStageControl()
	val writeStageControl  = WriteBackStageControl()
}


