package rc800.decoder

import spinal.core._

import rc800.registers.RegisterName
import rc800.registers.RegisterFileControl

object WriteSource extends SpinalEnum {
	val alu, memory = newElement()
}

case class WriteBackControl() extends Bundle {
	val source      = WriteSource()
	val intActive   = Bool
	val fileControl = Vec(RegisterFileControl(), 4)

	def fileControl(name: RegisterName.C): RegisterFileControl =
		name.mux(
			RegisterName.ft -> fileControl(0),
			RegisterName.bc -> fileControl(1),
			RegisterName.de -> fileControl(2),
			RegisterName.hl -> fileControl(3),
		)
}
