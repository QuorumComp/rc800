package rc800.decoder

import spinal.core._

object WriteMask extends SpinalEnum(defaultEncoding = binarySequential) {
	val none, low, high, full = newElement()
}

case class RegisterControl() extends Bundle {
	val write = Bool
	val push  = Bool
	val pop   = Bool
	val swap  = Bool
	val mask  = WriteMask()
}


