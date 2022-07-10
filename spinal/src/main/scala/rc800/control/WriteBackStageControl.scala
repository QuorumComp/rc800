package rc800.control

import spinal.core._

import rc800.control.component.RegisterFileControl

import rc800.registers.RegisterName


object WriteBackValueSource extends SpinalEnum(defaultEncoding = binarySequential) {
	val alu, memory = newElement()
}


case class WriteBackStageControl() extends Bundle {
	val source      = WriteBackValueSource()
	val fileControl = RegisterFileControl()
}
