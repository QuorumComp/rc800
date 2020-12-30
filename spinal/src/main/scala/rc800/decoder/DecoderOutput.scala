package rc800.decoder

import spinal.core._
import spinal.lib._

case class DecoderOutput() extends Bundle {
	val readStageControl   = ReadRegisterControl()
	val aluStageControl    = AluControl()
	val memoryStageControl = MemoryStageControl()
	val pcControl          = PcControl()
	val writeStageControl  = WriteBackControl()

	val intEnable = Bool
	val nmiActive = Bool
	val intActive = Bool
	val sysActive = Bool

	writeStageControl.intActive := nmiActive || intActive || sysActive
}
