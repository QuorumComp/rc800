package rc800.decoder

import spinal.core._
import spinal.lib._

object ValueSource extends SpinalEnum {
	val register1, register2, pc = newElement()
}

case class MemoryStageControl() extends Bundle {
	val enable  = Bool
	val write   = Bool
	val io      = Bool
	val code    = Bool
	val config  = Bool
	val push    = Bits(4 bits)
	val pop     = Bits(4 bits)
	val data    = ValueSource()
	val address = ValueSource()
}

