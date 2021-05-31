package rc800.control

import spinal.core._
import spinal.core.sim._
import spinal.lib._

case class RC811Control() extends Bundle {
	val stageControl = PipelineControl()

	val intEnable = Bool
	val nmiActive = Bool
	val intActive = Bool
	val sysActive = Bool

	def anyActive = nmiActive || intActive || sysActive
}
