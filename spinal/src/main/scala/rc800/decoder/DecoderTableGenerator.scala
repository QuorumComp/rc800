package rc800.decoder

import rc800.utils._

import spinal.core._
import spinal.sim._
import spinal.core.sim._

import scala.util.Random

import java.io._

case class ClockedOpcodeDecoder() extends Component {
	val io = new Bundle {
		val opcode = in Bits(8 bits)
		val controlSignals = out UInt(61 bits)
		val width = out UInt(8 bits)
	}

	val registeredOpcode = RegNext(io.opcode)

	val opcodeDecoder = OpcodeDecoder()
	opcodeDecoder.io.opcode := registeredOpcode

	io.controlSignals := opcodeDecoder.io.output.asBits.asUInt
	io.width := opcodeDecoder.io.output.asBits.getWidth
}


object DecoderSim {
	def printValue(value: BaseType): Unit = {
		println(s"${value.getName()} = ${value.toBigInt}")
	}

	def printBundle(bundle: Bundle): Unit = {
		bundle.flatten.foreach(printValue)
	}

	def printBits(bits: Bits): Unit = {
		for (i <- 0 until bits.getWidth) {
			print(bits(i).toBoolean)
		}
		println("")
	}

	def printBigInt(bigInt: BigInt, width: Int): Unit = {
		print("B\"")
		for (bit <- width -1 downto 0) {
			print(if (bigInt.testBit(bit)) 1 else 0)
		}
		println("\",")
	}

	def main(args: Array[String]) {
		SimConfig
		.compile(new ClockedOpcodeDecoder())
		.doSimUntilVoid { dut =>
			dut.clockDomain.forkStimulus(period = 10)
			dut.clockDomain.waitSampling()

			println(s"Width = ${dut.io.width.toInt}")

 			for (idx <- 0 to 255) {
				dut.clockDomain.waitRisingEdge()
				dut.io.opcode #= idx
				dut.clockDomain.waitRisingEdge()
				dut.clockDomain.waitRisingEdge()
				printBigInt(dut.io.controlSignals.toBigInt, dut.io.width.toInt)
			}
		}
	}
}
