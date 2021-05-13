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
		fileControl(name.as(UInt))
}
