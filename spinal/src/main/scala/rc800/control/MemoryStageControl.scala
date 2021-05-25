package rc800.control

import spinal.core._
import spinal.lib._


object MemoryStageAddressSource extends SpinalEnum {
	val register1, pc = newElement()
}


case class MemoryStageControl() extends Bundle {
	val enable  = Bool
	val write   = Bool
	val io      = Bool
	val code    = Bool
	val config  = Bool
	val address = MemoryStageAddressSource()
}

