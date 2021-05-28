package rc800.decoder

import rc800.Utils._

import spinal.core._
import spinal.sim._
import spinal.core.sim._

import scala.util.Random

import java.io._


object DecoderSim {
	def main(args: Array[String]) {
		SimConfig
		.withWave
		.compile(new OpcodeDecoder())
		.doSim { dut =>
			for (idx <- 0 to 255) {
				dut.io.opcode #= idx
				println(dut.controlBits.toString)
			}
		}
	}
}
