package rc800.decoder

import spinal.core._

import rc800.registers.Register
import rc800.registers.WritePart
import rc800.registers.RegisterFileOperation

object WriteSource extends SpinalEnum {
	val alu, memory = newElement()
}

case class WriteBackControl() extends Bundle {
	val enable    = Bool
	val source    = WriteSource()
	val intActive = Bool
	val operation = RegisterFileOperation()
	val register  = Register()
	val part      = WritePart()
}
