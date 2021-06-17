package rc800.lpm.blackbox

import spinal.core._
import spinal.lib._

import rc800.lpm

class CLShift(shiftType: lpm.CLShift.ShiftType.Value, dataWidth: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left)
	extends BlackBox
	with lpm.CLShift {

	override def width = dataWidth

    addGeneric("LPM_SHIFTTYPE", shiftType.toString())
    addGeneric("LPM_WIDTH", width)
	addGeneric("LPM_WIDTHDIST", log2Up(width))
	addGeneric("LPM_TYPE", "LPM_CLSHIFT")

	setBlackBoxName("LPM_CLSHIFT")

	noIoPrefix()
}
