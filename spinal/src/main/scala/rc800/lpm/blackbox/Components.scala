package rc800.lpm.blackbox

import rc800.lpm

object Components extends lpm.Components {
	def clShift(shiftType: lpm.CLShift.ShiftType.Value, width: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left): CLShift =
		new CLShift(shiftType, width, direction)		
}
