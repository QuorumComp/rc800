package rc800.control.component

import spinal.core._
import rc800.registers._


case class RegisterFileControl() extends Bundle {
	val write = Bool()
	val writeRegister = RegisterName()

	val writeAux = Bool()
	val writeAuxRegister = RegisterName()
	val auxFromMemory = Bool()

	val registerControl = Vec(RegisterControl(), 4)

	def registerControl(name: RegisterName.C): RegisterControl =
		registerControl(name.asBits(1 downto 0).asUInt)
}
