package rc800.control.component

import spinal.core._


case class RegisterFileControl() extends Bundle {
	val registerControl = RegisterControl()
	val rot8      = Bool
	val sourceExg = Bool
}
