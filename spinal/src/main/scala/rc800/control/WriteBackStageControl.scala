package rc800.control

import spinal.core._

import rc800.control.component.RegisterFileControl

import rc800.registers.RegisterName


object WriteBackValueSource extends SpinalEnum(defaultEncoding = binarySequential) {
	val alu, memory = newElement()
}


case class WriteBackStageControl() extends Bundle {
	val source      = WriteBackValueSource()
	val fileControl = Vec(RegisterFileControl(), 4)

	def fileControl(name: RegisterName.C): RegisterFileControl =
		fileControl(name.as(UInt))
}
