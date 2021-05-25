package rc800.registers

import spinal.core._


object RegisterName extends SpinalEnum(defaultEncoding = binarySequential) {
	val ft, bc, de, hl = newElement()
}
