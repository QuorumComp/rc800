package rc800

import spinal.core._
import spinal.lib._

object Vectors {
	val Reset = 0x0000
	val NonMaskableInterrupt = 0x0008
	val IllegalInterrupt = 0x0010
	val IllegalInstruction = 0x0018
	val StackOverflow = 0x0020
	val ExternalInterrupt = 0x0028
}
