package rc800

import spinal.core._

// Generates standalone RC811 Verilog using the pure-Scala (generic) LPM so the
// result is fully simulatable in iverilog (no unimplemented blackbox modules).
object RC811Gen {
	def main(args: Array[String]) {
		SpinalVerilog(new RC811()(lpm.generic.Components)).printPruned()
	}
}
