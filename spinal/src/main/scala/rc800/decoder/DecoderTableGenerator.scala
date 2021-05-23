package rc800.decoder

import rc800.Utils._

import spinal.core._
import spinal.sim._
import spinal.core.sim._

import scala.util.Random

import java.io._


//MyTopLevel's testbench
object DecoderSim {
	/*
	def main(args: Array[String]) {
		SimConfig
		.withWave
		.compile(new Decoder())
		.doSim { dut =>
			// Fork a process to generate the reset and the clock on the dut
			dut.clockDomain.forkStimulus(period = 10)

			dut.clockDomain.waitSampling()

			var modelState = 0
			for (idx <- 0 to 600) {
				dut.clockDomain.waitFallingEdge()

				val pc = dut.pc.toInt
				val stage = dut.stage.toInt

				if (dut.io.busEnable.toBoolean) {
					if (dut.io.write.toBoolean) {
						val writer: (Int, Int) => Unit = if (dut.io.io.toBoolean) writeIo else writeMemory
						writer(dut.io.address.toInt, dut.io.dataOut.toInt)
					} else {
						val reader: (Int, Boolean) => Int = if (dut.io.io.toBoolean) readIo else readMemory
						dut.io.dataIn #= reader(dut.io.address.toInt, dut.io.code.toBoolean) & 0xFF
					}
				}
			}
		}
	}
	*/
}
