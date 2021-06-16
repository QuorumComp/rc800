package rc800.lpm.generic

import spinal.core._
import spinal.lib._

import rc800.lpm

case class CLShift(shiftType: lpm.CLShift.ShiftType.Value, dataWidth: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left)
	extends Component
	with lpm.CLShift {

	override def width = dataWidth

	if (shiftType == lpm.CLShift.ShiftType.rotate) {
		io.result := io.data.rotateLeft(io.distance)
	}
}
