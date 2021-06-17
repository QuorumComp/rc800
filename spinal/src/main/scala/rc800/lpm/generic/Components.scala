package rc800.lpm.generic

import rc800.lpm

object Components extends lpm.Components {
	def clShift(shiftType: lpm.CLShift.ShiftType.Value, width: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left): CLShift =
		new CLShift(shiftType, width, direction)		

	def addSub(dataWidth: Int, representation: lpm.AddSub.Representation.Value, direction: lpm.AddSub.Direction.Value): AddSub =
		new AddSub(dataWidth, representation, direction)
}
