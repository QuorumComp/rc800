package rc800.lpm.blackbox

import spinal.core._
import spinal.lib._

import rc800.lpm


class AddSub(dataWidth: Int, representation: lpm.AddSub.Representation.Value, operation: lpm.AddSub.Direction.Value)
	extends BlackBox
	with lpm.AddSub {

	override def width = dataWidth
	override def direction = operation

	addGeneric("LPM_TYPE", "LPM_ADD_SUB")
    addGeneric("LPM_WIDTH", width)
	addGeneric("LPM_PIPELINE", 0)
	addGeneric("LPM_REPRESENTATION", representation.toString())
	addGeneric("LPM_HINT", "UNUSED")

	if (direction != lpm.AddSub.Direction.dynamic)
		addGeneric("LPM_DIRECTION", direction.toString())

	setBlackBoxName("LPM_ADD_SUB")

	noIoPrefix()
}
