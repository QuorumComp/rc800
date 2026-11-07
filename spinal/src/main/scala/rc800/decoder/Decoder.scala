package rc800.decoder

import spinal.core._

import rc800.alu.AluOperation
import rc800.alu.Condition
import rc800.alu.OperandSource
import rc800.alu.ShiftOperation

import rc800.control.PipelineControl
import rc800.control.RC811Control

import rc800.lpm

import rc800.registers.RegisterName

import rc800.Vectors


case class DecoderOutput() extends Bundle {
	val stageControl = PipelineControl()
	val illegal = Bool
	val overflow = Bool
}


case class Decoder(implicit lpmComponents: lpm.Components) extends Component {
	import Pipeline._

	val io = new Bundle {
		val strobe = in Bool()
		val opcodeAsync = in Bits(8 bits)

		val nmiReq    = in Bool()
		val intReq    = in Bool()

		val stackPointers = in Vec(UInt(8 bits), 4)

		val intEnable = in Bool()
		val nmiActive = in Bool()
		val intActive = in Bool()
		val sysActive = in Bool()

		val output = out (RC811Control())
	}

	private val useLookup = true	// experiment: ROM decoder vs 2024.1 mis-synthesis

	private val opcodeIn = RegNextWhen(io.opcodeAsync, io.strobe) init(0)
	private val opcode = io.strobe ? io.opcodeAsync | opcodeIn
	private val opcodeOut = Bits(8 bits)
	opcodeOut := opcode

	private def lookupDecoder = {
		// Synchronous read: the table's own output register is the pipeline
		// register that holds the control word for the window which executes
		// it, which is what lets the table infer as block RAM.  See
		// BRAM-DECODE-PLAN.md; the chain flavour below reproduces this timing
		// contract explicitly so both decoders are interchangeable.
		val v = LookupDecoder()
		v.io.opcode <> opcodeOut
		v.io.strobe <> io.strobe
		v.io.nmiActive := io.nmiActive
		v.io.controlSignals <> io.output.stageControl
		v.io.output.illegal <> decoderIllegal
		v.io.output.overflow <> decoderOverflow
		v
	}

	private def opcodeDecoder = {
		val v = OpcodeDecoder()
		v.io.opcode <> opcodeOut
		v.io.nmiActive := io.nmiActive
		v.io.stackPointers := io.stackPointers
		// The chain is combinational, so hold its word to the same schedule
		// the ROM's output register gives the table flavour: captured on the
		// strobe that admits the opcode, valid for the rest of the window.
		val chainControl = cloneOf(io.output.stageControl)
		chainControl := v.io.controlSignals
		io.output.stageControl := RegNextWhen(chainControl, io.strobe)
		v.io.output.illegal <> decoderIllegal
		v.io.output.overflow <> decoderOverflow
		v
	}

	val decoderIllegal = Bool()
	val decoderOverflow = Bool()
	val decoder = if (useLookup) lookupDecoder else opcodeDecoder

	// The ROM table cannot see the stack pointers, so the zero-SP faults that
	// OpcodeDecoder raises inline for PUSH and PUSHA have to be raised here
	// instead.  The fault word replaces the table word's pushes: only the
	// vector's HL push survives, exactly as in the OpcodeDecoder fault path.
	if (useLookup) {
		val pushPair = (B"10" ## opcode(1 downto 0)).as(RegisterName())
		def stackFault(vector: Int): Unit = {
			decoderOverflow := True
			for(r <- Seq(RegisterName.ft, RegisterName.bc, RegisterName.de, RegisterName.hl))
				io.output.stageControl.writeStageControl.fileControl.registerControl(r).push := False
			io.output.stageControl.interrupt(U(vector >> 3, 3 bits))
		}
		when(!io.nmiActive) {
			when(opcode === Opcodes.PUSHA && (0 to 3).map(i => io.stackPointers(i) === 0).reduce(_ || _)) {
				stackFault(Vectors.StackOverflow)
			}
			when(opcode === Opcodes.PUSH && io.stackPointers(pushPair.asBits(1 downto 0).asUInt) === 0) {
				stackFault(Vectors.StackOverflow)
			}
		}
	}

	private val anyActive = io.nmiActive || io.intActive || io.sysActive

	private val reqExtInt = io.intReq && io.intEnable && !io.intActive && !io.nmiActive

	private val performInterrupt = Bool()
	private val interruptVector = UInt(3 bits)
	
	def setDefaults(): Unit = {
		io.output.intEnable := io.intEnable
		io.output.nmiActive := io.nmiActive || decoderIllegal || decoderOverflow
		io.output.intActive := io.intActive
		io.output.sysActive := io.sysActive
		performInterrupt := False
		interruptVector := 0
	}

	setDefaults()

	def cancel(): Unit = {
		opcodeOut := Opcodes.NOP_opcode
	}

	when (performInterrupt) {
		io.output.stageControl.interrupt(interruptVector)
	}

	def interrupt(vector: Int) {
		interruptVector := vector >> 3
		performInterrupt := True
	}

	when (io.nmiReq) {
		io.output.nmiActive := True
		interrupt(Vectors.NonMaskableInterrupt)
		cancel()
	}.elsewhen (reqExtInt) {
		io.output.intActive := True
		interrupt(Vectors.ExternalInterrupt)
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
			interrupt(Vectors.IllegalInterrupt)
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
			interrupt(Vectors.IllegalInterrupt)

			cancel()
		}
	}
}

