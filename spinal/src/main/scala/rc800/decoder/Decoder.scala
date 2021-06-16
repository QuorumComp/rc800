package rc800.decoder

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.ShiftOperation

import rc800.control.PipelineControl
import rc800.control.RC811Control

import rc800.Vectors


case class DecoderOutput() extends Bundle {
	val stageControl = PipelineControl()
	val nmi = Bool
}


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

	private val useLookup = true

	private val opcodeIn = RegNextWhen(io.opcodeAsync, io.strobe) init(0)
	private val opcode = io.strobe ? io.opcodeAsync | opcodeIn
	private val opcodeOut = Bits(8 bits)
	opcodeOut := opcode

	private def lookupDecoder = {
		val v = LookupDecoder()
		v.io.opcode <> opcodeOut
		v.io.strobe <> True
		v.io.controlSignals <> io.output.stageControl
		v.io.output.nmi <> decoderNmi
		v
	}

	private def opcodeDecoder = {
		val v = OpcodeDecoder()
		v.io.opcode <> opcodeOut
		v.io.controlSignals <> io.output.stageControl
		v.io.output.nmi <> decoderNmi
		v
	}

	val decoderNmi = Bool()
	val decoder = if (useLookup) lookupDecoder else opcodeDecoder

	private val anyActive = io.nmiActive || io.intActive || io.sysActive

	private val reqExtInt = io.intReq && io.intEnable && !io.intActive && !io.nmiActive

	def setDefaults(): Unit = {
		io.output.intEnable := io.intEnable
		io.output.nmiActive := io.nmiActive || decoderNmi
		io.output.intActive := io.intActive
		io.output.sysActive := io.sysActive
	}

	setDefaults()

	def cancel(): Unit = {
		opcodeOut := Opcodes.NOP_opcode
	}

	when (io.nmiReq) {
		io.output.nmiActive := True
		io.output.stageControl.interrupt(Vectors.NonMaskableInterrupt)
		cancel()
	}.elsewhen (reqExtInt) {
		io.output.intActive := True
		io.output.stageControl.interrupt(Vectors.ExternalInterrupt)
		cancel()
	}.otherwise {
		switch (opcode) {
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
			cancel()
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

			cancel()
		}
	}
}

