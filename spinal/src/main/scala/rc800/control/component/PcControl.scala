package rc800.control.component

import spinal.core._
import spinal.lib._

import rc800.alu.PcCondition
import rc800.alu.PcTruePathSource


case class PcControl() extends Bundle {
	val condition     = PcCondition()
	val truePath      = PcTruePathSource()
	val decodedOffset = UInt(1 bits)
	val vector        = UInt(3 bits)
}


