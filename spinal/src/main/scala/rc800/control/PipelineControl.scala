package rc800.control

import spinal.core._
import spinal.lib._


case class PipelineControl() extends Bundle {
	val readStageControl   = ReadRegisterStageControl()
	val memoryStageControl = MemoryStageControl()
	val aluStageControl    = AluStageControl()
	val writeStageControl  = WriteBackStageControl()
}


