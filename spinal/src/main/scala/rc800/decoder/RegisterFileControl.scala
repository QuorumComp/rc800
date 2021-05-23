package rc800.decoder

import spinal.core._


object RegisterName extends SpinalEnum(defaultEncoding = binarySequential) {
	val ft, bc, de, hl = newElement()
}


case class RegisterFileControl() extends Bundle {
	val registerControl = RegisterControl()
	val rot8      = Bool
	val sourceExg = Bool
}


