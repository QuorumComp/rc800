package rc800.decoder

import spinal.core._
import spinal.lib._


object PcTruePath extends SpinalEnum {
	val offsetFromMemory,
		offsetFromDecoder,
		register1,
		register2,
		vectorFromMemory,
		vectorFromDecoder = newElement()
}

object PcCondition extends SpinalEnum {
	val always,
		whenConditionMet,
		whenResultNotZero = newElement()
}

case class PcControl() extends Bundle {
	val condition     = PcCondition()
	val truePath      = PcTruePath()
	val decodedOffset = UInt(1 bits)
	val vector        = UInt(3 bits)
}


