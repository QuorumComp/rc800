package rc800.lpm.blackbox

import rc800.lpm

object Components extends lpm.Components {
	def clShift(shiftType: lpm.CLShift.ShiftType.Value, width: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left): CLShift =
		new CLShift(shiftType, width, direction)		

	def addSub(dataWidth: Int, representation: lpm.AddSub.Representation.Value, direction: lpm.AddSub.Direction.Value): AddSub =
		new AddSub(dataWidth, representation, direction)

	// This family is the Altera LPM set, and there is no LPM ROM model in the
	// tree to instantiate; the Xilinx primitive family has its own object.
	// Inferred memory is what every build in this repo has always used.
	def rom(dataWidth: Int, content: Seq[BigInt]): lpm.Rom =
		new rc800.lpm.generic.Rom(dataWidth, content)
}
