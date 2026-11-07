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
		val add_sub = (direction == Direction.dynamic) generate (in Bool());

		val result = out Bits(width bits)
		val cout = out Bool()
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


trait Rom extends Component {
	def width: Int = ???
	def depth: Int = ???

	// Contents, one number per address, in address order.  A generic
	// implementation builds an inferred memory from it; a vendor primitive
	// takes the same numbers as its initialisation attributes, so the content
	// travels with the component instead of living in a file someone has to
	// keep in step by hand.
	def init: Seq[BigInt] = ???

	val io = new Bundle {
		val enable  = in Bool()
		val address = in UInt(log2Up(depth) bits)
		// Registered: the read answers the address held when enable was
		// asserted, which is what lets the caller treat the output as its own
		// stage register rather than adding one of its own.
		val data    = out Bits(width bits)
	}
}


trait Components {
	def clShift(shiftType: CLShift.ShiftType.Value, width: Int, direction: CLShift.ShiftDirection.Value = CLShift.ShiftDirection.left): CLShift
	def addSub(dataWidth: Int, representation: AddSub.Representation.Value, direction: AddSub.Direction.Value): AddSub
	def rom(dataWidth: Int, content: Seq[BigInt]): Rom
} 
