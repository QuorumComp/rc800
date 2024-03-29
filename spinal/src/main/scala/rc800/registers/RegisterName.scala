package rc800.registers

import spinal.core._


object RegisterName extends SpinalEnum(defaultEncoding = binarySequential) {
	val f, t, b, c, d, e, h, l,
		ft, bc, de, hl = newElement()

	def is16bit(register: RegisterName.C): Bool =
		register.asBits(3)
}
