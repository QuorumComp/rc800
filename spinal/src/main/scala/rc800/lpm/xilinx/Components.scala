package rc800.lpm.xilinx

import rc800.lpm

/** Xilinx flavoured component set: the portable implementations everywhere the
  * toolchain has no better option, and a block-memory ROM where inference from
  * a file-loaded initialisation would have left logic instead. */
object Components extends lpm.Components {
	def clShift(shiftType: lpm.CLShift.ShiftType.Value, width: Int, direction: lpm.CLShift.ShiftDirection.Value = lpm.CLShift.ShiftDirection.left): lpm.CLShift =
		new rc800.lpm.generic.CLShift(shiftType, width, direction)

	def addSub(dataWidth: Int, representation: lpm.AddSub.Representation.Value, direction: lpm.AddSub.Direction.Value): lpm.AddSub =
		new rc800.lpm.generic.AddSub(dataWidth, representation, direction)

	def rom(dataWidth: Int, content: Seq[BigInt]): lpm.Rom =
		new Rom(dataWidth, content)
}
