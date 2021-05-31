package rc800.decoder

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.ShiftOperation

import rc800.control.RC811Control

import rc800.Vectors


case class Decoder() extends Component {
	import Pipeline._

	val io = new Bundle {
		val strobe = in Bool
		val opcodeAsync = in Bits(8 bits)

		val nmiReq    = in Bool
		val intReq    = in Bool
		val intEnable = in Bool
		val nmiActive = in Bool
		val intActive = in Bool
		val sysActive = in Bool

		val output = out (RC811Control())
	}

	private val useLookup = false

	private val opcode = if (useLookup) io.opcodeAsync else RegNextWhen(io.opcodeAsync, io.strobe)

	//val opcodeDecoder = LookupDecoder()
	val opcodeDecoder = OpcodeDecoder()
	opcodeDecoder.io.opcode <> opcode
	opcodeDecoder.io.controlSignals <> io.output.stageControl

	private val anyActive = io.nmiActive || io.intActive || io.sysActive

	private val reqExtInt = io.intReq && io.intEnable && !io.intActive && !io.nmiActive

	def setDefaults(): Unit = {
		io.output.intEnable := io.intEnable
		io.output.nmiActive := io.nmiActive
		io.output.intActive := io.intActive
		io.output.sysActive := io.sysActive
	}

	setDefaults()

	when (io.nmiReq) {
		io.output.nmiActive := True
		io.output.stageControl.interrupt(Vectors.NonMaskableInterrupt)
		opcode := Opcodes.NOP_opcode
	}.elsewhen (reqExtInt) {
		io.output.intActive := True
		io.output.stageControl.interrupt(Vectors.ExternalInterrupt)
		opcode := Opcodes.NOP_opcode
	}.otherwise {
		switch (opcode) {
			for (op <- Opcodes.illegals) {
				is (op) { 
					io.output.nmiActive := True
					io.output.stageControl.interrupt(Vectors.IllegalInstruction)
					opcode := Opcodes.NOP_opcode
				}
			}

			// Opcodes with no fields
			is (Opcodes.DI)    { io.output.intEnable := False}
			is (Opcodes.EI)    { io.output.intEnable := True }
			is (Opcodes.RETI)  { reti() }
			is (Opcodes.SYS_I) { sys() }
		}
	}

	def sys(): Unit = {
		when (anyActive) {
			io.output.nmiActive := True
			io.output.stageControl.interrupt(Vectors.IllegalInterrupt)
			opcode := Opcodes.NOP_opcode
		}.otherwise {
			io.output.sysActive := True
		}
	}

	def reti(): Unit = {
		when (anyActive) {
			when (io.nmiActive) {
				io.output.nmiActive := False
			}.elsewhen (io.intActive) {
				io.output.intActive := False
			}.otherwise /* handlingSys */ {
				io.output.sysActive := False
			}
		}.otherwise {
			io.output.nmiActive := True
			io.output.stageControl.interrupt(Vectors.IllegalInterrupt)

			opcode := Opcodes.NOP_opcode
		}
	}
}

