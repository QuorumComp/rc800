package rc800.lpm

import spinal.core._
import spinal.lib._

trait CLShift extends Component {
	import CLShift._

	def width: Int = ???

	val io = new Bundle {
		val data = in Bits(width bits)
		val distance = in UInt(log2Up(width) bits)
		//val direction = in Bool

		val result = out Bits(width bits)
	}
}

object CLShift {
	object ShiftType extends Enumeration {
		val rotate = Value("ROTATE")
		val logical = Value("LOGICAL")
		val arithmetic = Value("ARITHMETIC")
	}

	object ShiftDirection extends Enumeration {
		val left, right = Value
	}
}


trait AddSub extends Component {
	import AddSub._

	def width: Int = ???
	def direction: Direction.Value = ???

	val io = new Bundle {
		val dataa = in Bits(width bits)
		val datab = in Bits(width bits)
		val add_sub = (direction == Direction.dynamic) generate (in Bool);

		val result = out Bits(width bits)
		val cout = out Bool
	}
}

object AddSub {
	object Representation extends Enumeration {
		val signed = Value("SIGNED")
		val unsigned = Value("UNSIGNED")
	}

	object Direction extends Enumeration {
		val add = Value("ADD")
		val sub = Value("SUB")
		val dynamic = Value("DEFAULT")
	}
}


trait Components {
	def clShift(shiftType: CLShift.ShiftType.Value, width: Int, direction: CLShift.ShiftDirection.Value = CLShift.ShiftDirection.left): CLShift
	def addSub(dataWidth: Int, representation: AddSub.Representation.Value, direction: AddSub.Direction.Value): AddSub
} 
