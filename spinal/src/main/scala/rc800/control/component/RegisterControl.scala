package rc800.control.component

import spinal.core._

case class RegisterControl() extends Bundle {
	val write = Bool
	val push  = Bool
	val pop   = Bool
	val swap  = Bool
}


