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
		protected case class ShiftTypeVal(val stringRepresentation: String) extends super.Val

		import scala.language.implicitConversions
		implicit def valueToShiftTypeVal(x: Value): ShiftTypeVal = x.asInstanceOf[ShiftTypeVal]

		val rotate = ShiftTypeVal("ROTATE")
		val logical = ShiftTypeVal("LOGICAL")
		val arithmetic = ShiftTypeVal("ARITHMETIC")
	}

	object ShiftDirection extends Enumeration {
		val left, right = Value
	}
}


trait Components {
	def clShift(shiftType: CLShift.ShiftType.Value, width: Int, direction: CLShift.ShiftDirection.Value = CLShift.ShiftDirection.left): CLShift
} 
