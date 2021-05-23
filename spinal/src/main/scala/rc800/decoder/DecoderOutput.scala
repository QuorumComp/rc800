package rc800.decoder

import spinal.core._
import spinal.lib._

case class DecoderOutput() extends Bundle {
	val stageControl = StageControl()

	val intEnable = Bool
	val nmiActive = Bool
	val intActive = Bool
	val sysActive = Bool

	def anyActive = nmiActive || intActive || sysActive
}
