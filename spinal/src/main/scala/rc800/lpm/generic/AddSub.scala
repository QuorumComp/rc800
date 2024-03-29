package rc800.lpm.generic

import spinal.core._
import spinal.lib._

import rc800.lpm


class AddSub(dataWidth: Int, representation: lpm.AddSub.Representation.Value, operation: lpm.AddSub.Direction.Value)
	extends Component
	with lpm.AddSub {

	override def width = dataWidth
	override def direction = operation

	private val isAdd = if (operation == lpm.AddSub.Direction.dynamic) io.add_sub else Bool(operation == lpm.AddSub.Direction.add)
	private val operand1 = io.dataa ## False
	private val operand2 = (isAdd ? ~io.datab | io.datab) ## isAdd
	private val subResult = operand1.asUInt -^ operand2.asUInt

	val result = subResult(dataWidth downto 1)
	val carry = subResult(dataWidth + 1)

	io.cout := ~carry
	io.result := result.asBits

}
