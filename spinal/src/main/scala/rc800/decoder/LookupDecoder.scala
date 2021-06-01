package rc800.decoder

import spinal.core._

import rc800.alu.Condition
import rc800.control.PipelineControl
import rc800.control.AluStageControl


case class LookupDecoder() extends Component {
	val io = new Bundle {
		val opcode = in Bits(8 bits)
		val strobe = in Bool
		val controlSignals = out (PipelineControl())
	}

	val content = Array(
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000001101000001",
		B"000000000000000000000000000000000001010001000000010001001000001101000010",
		B"000000000000000000000000000000000001010001000000010001001000001101000011",
		B"000000000000000000000000010100011001010001000000010001001000000100000000",
		B"000000000000000000000000010100011001010001000000010001001000000100000001",
		B"000000000000000000000000010100011001010001000000010001001000000100000010",
		B"000000000000000000000000010100011001010001000000010001001000000100000011",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001001001101010001",
		B"000000000000000000000000010100011001010001000000010001001001000100010001",
		B"000000000000000000000000010100011001010001000000010001001000100100000000",
		B"000000000000000000000000010100011001010001000000010001001000100100000001",
		B"000000000000000000000000010100011001010001000000010001001000100100000010",
		B"000000000000000000000000010100011001010001000000010001001000100100000011",
		B"000000000000000000000000011100010001010100000000010001001000000000000000",
		B"000000000000000000000000110100010001010100000000010001001000000001010000",
		B"000000000000000010100001010100010001010100000000010001001000000001100001",
		B"000000000000000011010001010100010001010100000000010001001000000001010001",
		B"000000001010000100000000010100010001010100000000010001001000000001100010",
		B"000000001101000100000000010100010001010100000000010001001000000001010010",
		B"101000010000000000000000010100010001010100000000010001001000000001100011",
		B"110100010000000000000000010100010001010100000000010001001000000001010011",
		B"000000000000000000000000001000010001010010000000010001000100000010000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000001110000000",
		B"000000000000000000000000000000000001010001000000010001001000001101000000",
		B"000000000000000000000000000000000001010001000000010001001000001110000100",
		B"000000000000000000000000000000000001010001000000010001001000001101000100",
		B"000000000000000000000000000000000001010001000000010001001000001110001000",
		B"000000000000000000000000000000000001010001000000010001001000001101001000",
		B"000000000000000000000000000000000001010001000000010001001000001110001100",
		B"000000000000000000000000000000000001010001000000010001001000001101001100",
		B"000000000000000000000000001000011001010001000000010001001000000100000000",
		B"000000000000000000000000010100011001010001000000010001001000000100000000",
		B"000000000000000000100001000000001001010001000000010001001000000100000000",
		B"000000000000000001010001000000001001010001000000010001001000000100000000",
		B"000000000010000100000000000000001001010001000000010001001000000100000000",
		B"000000000101000100000000000000001001010001000000010001001000000100000000",
		B"001000010000000000000000000000001001010001000000010001001000000100000000",
		B"010100010000000000000000000000001001010001000000010001001000000100000000",
		B"000000000000000000000000000000000001010001000000010001001000011101000000",
		B"000000000000000000000000000000000001010001000000010001001000011101000001",
		B"000000000000000000000000000000000001010001000000010001001000011101000010",
		B"000000000000000000000000000000000001010001000000010001001000011101000011",
		B"000000000000000000000000010100011001010001000000010001001000010100000000",
		B"000000000000000000000000010100011001010001000000010001001000010100000001",
		B"000000000000000000000000010100011001010001000000010001001000010100000010",
		B"000000000000000000000000010100011001010001000000010001001000010100000011",
		B"001100010000000000000000000000000001010100000000100001001100000000000000",
		B"001100010000000000000000000000000001010100000000100001001100000000000100",
		B"001100010000000000000000000000000001010100000000100001001100000000001000",
		B"001100010000000000000000000000000001010100000000100001001100000000001100",
		B"000000000000000000000000000000000001010001000000100001001000000000000000",
		B"000000000000000000000000000000000001010001000000100001001000000000000100",
		B"000000000000000000000000000000000001010001000000100001001000000000001000",
		B"000000000000000000000000000000000001010001000000100001001000000000001100",
		B"000000000000000000000000010100010001010000000000010001001000000010010000",
		B"000000000000000000000000010100010001010000000000010001001000000001010000",
		B"000000000000000000000000010100010001010000000000010001001000000010010100",
		B"000000000000000000000000010100010001010000000000010001001000000001010100",
		B"000000000000000000000000010100010001010000000000010001001000000010011000",
		B"000000000000000000000000010100010001010000000000010001001000000001011000",
		B"000000000000000000000000010100010001010000000000010001001000000010011100",
		B"000000000000000000000000010100010001010000000000010001001000000001011100",
		B"000000000000000000000000001000010001010011100000010001001000000010010000",
		B"000000000000000000000000001000010001010011000000010001001000000000010000",
		B"000000000000000000000000001000010001010011100000010001001000000010010100",
		B"000000000000000000000000001000010001010011100000010001001000000001010100",
		B"000000000000000000000000001000010001010011100000010001001000000010011000",
		B"000000000000000000000000001000010001010011100000010001001000000001011000",
		B"000000000000000000000000001000010001010011100000010001001000000010011100",
		B"000000000000000000000000001000010001010011100000010001001000000001011100",
		B"000000000000000000000000010100010001010000100000010001001000000010010000",
		B"000000000000000000000000010100010001010000100000010001000000000001000000",
		B"000000000000000000000000010100010001010000100000010001001000000010010100",
		B"000000000000000000000000010100010001010000100000010001001000000001010100",
		B"000000000000000000000000010100010001010000100000010001001000000010011000",
		B"000000000000000000000000010100010001010000100000010001001000000001011000",
		B"000000000000000000000000010100010001010000100000010001001000000010011100",
		B"000000000000000000000000010100010001010000100000010001001000000001011100",
		B"000000000000000000000000010100010001010100000000010001001000000000100000",
		B"000001000000000000000000000000000001010001000000100001001000000000001100",
		B"000000000000000000000000010100010001010100000000010001001000000000100001",
		B"000000000000000000000000010100010001010100000000010001001000000000010001",
		B"000000000000000000000000010100010001010100000000010001001000000000100010",
		B"000000000000000000000000010100010001010100000000010001001000000000010010",
		B"000000000000000000000000010100010001010100000000010001001000000000100011",
		B"000000000000000000000000010100010001010100000000010001001000000000010011",
		B"000000000000000000000000010100010001010001100000010001001000000010010000",
		B"000000000000000000000000010100010001010001100000010001001000000001010000",
		B"000000000000000000000000010100010001010001100000010001001000000010010100",
		B"000000000000000000000000010100010001010001100000010001001000000001010100",
		B"000000000000000000000000010100010001010001100000010001001000000010011000",
		B"000000000000000000000000010100010001010001100000010001001000000001011000",
		B"000000000000000000000000010100010001010001100000010001001000000010011100",
		B"000000000000000000000000010100010001010001100000010001001000000001011100",
		B"000000000000000000000000010100010001010001000000010001001000000010010000",
		B"000000000000000000000000010100010001010001000000010001001000000001010000",
		B"000000000000000000000000010100010001010001000000010001001000000010010100",
		B"000000000000000000000000010100010001010001000000010001001000000001010100",
		B"000000000000000000000000010100010001010001000000010001001000000010011000",
		B"000000000000000000000000010100010001010001000000010001001000000001011000",
		B"000000000000000000000000010100010001010001000000010001001000000010011100",
		B"000000000000000000000000010100010001010001000000010001001000000001011100",
		B"000000000000000000000000010100010001010010000000010001001000000010010000",
		B"000000000000000000000000010100010001010010000000010001001000000001010000",
		B"000000000000000000000000010100010001010010000000010001001000000010010100",
		B"000000000000000000000000010100010001010010000000010001001000000001010100",
		B"000000000000000000000000010100010001010010000000010001001000000010011000",
		B"000000000000000000000000010100010001010010000000010001001000000001011000",
		B"000000000000000000000000010100010001010010000000010001001000000010011100",
		B"000000000000000000000000010100010001010010000000010001001000000001011100",
		B"000000000000000000000000001000010001010100000000010001001000000000010000",
		B"000000000000000000000000010100010001010100000000010001001000000000010000",
		B"000000000000000000100001000000000001010100000000010001001000000000010000",
		B"000000000000000001010001000000000001010100000000010001001000000000010000",
		B"000000000010000100000000000000000001010100000000010001001000000000010000",
		B"000000000101000100000000000000000001010100000000010001001000000000010000",
		B"001000010000000000000000000000000001010100000000010001001000000000010000",
		B"010100010000000000000000000000000001010100000000010001001000000000010000",
		B"000000000000000000000000001000011001010001000010010001001010100100000000",
		B"000000000000000000000000010100011001010001000010010001001010100100000000",
		B"000000000000000000100001000000001001010001000010010001001010100100000000",
		B"000000000000000001010001000000001001010001000010010001001010100100000000",
		B"000000000010000100000000000000001001010001000010010001001010100100000000",
		B"000000000101000100000000000000001001010001000010010001001010100100000000",
		B"001000010000000000000000000000001001010001000010010001001010100100000000",
		B"010100010000000000000000000000001001010001000010010001001010100100000000",
		B"000000000000000000000000001000010001010000000000001000101010100100100000",
		B"000000000000000000000000010100010001010000000000001000101010100100010000",
		B"000000000000000000100001000000000001010000000000001000101010100100100001",
		B"000000000000000001010001000000000001010000000000001000101010100100010001",
		B"000000000010000100000000000000000001010000000000001000101010100100100010",
		B"000000000101000100000000000000000001010000000000001000101010100100010010",
		B"001000010000000000000000000000000001010000000000001000101010100100100011",
		B"010100010000000000000000000000000001010000000000001000101010100100010011",
		B"000000000000000000000000000000000000000001100000000100001010100100100000",
		B"000000000000000000000000000000000000001001100000000100001010100100100000",
		B"000000000000000000000000000000000000010001100000000100001010100100100000",
		B"000000000000000000000000000000000000011001100000000100001010100100100000",
		B"000000000000000000000000000000000000100001100000000100001010100100100000",
		B"000000000000000000000000000000000000101001100000000100001010100100100000",
		B"000000000000000000000000000000000000110001100000000100001010100100100000",
		B"000000000000000000000000000000000000111001100000000100001010100100100000",
		B"000000000000000000000000000000000001000001100000000100001010100100100000",
		B"000000000000000000000000000000000001001001100000000100001010100100100000",
		B"000000000000000000000000000000000001010001100000000100001010100100100000",
		B"001100110000000000000000000000000001010000100000110000101110100100000000",
		B"000000000000000000000000000000000001100001100000000100001010100100100000",
		B"000000000000000000000000000000000001101001100000000100001010100100100000",
		B"000000000000000000000000000000000001110001100000000100001010100100100000",
		B"000000000000000000000000000000000001111001100000000100001010100100100000",
		B"000000000000000000000000001000010001010000000010010010001010100100100000",
		B"000000000000000000000000010100010001010000000010010010001010100100010000",
		B"000000000000000000100001000000000001010000000010010010001010100100100001",
		B"000000000000000001010001000000000001010000000010010010001010100100010001",
		B"000000000010000100000000000000000001010000000010010010001010100100100010",
		B"000000000101000100000000000000000001010000000010010010001010100100010010",
		B"001000010000000000000000000000000001010000000010010010001010100100100011",
		B"010100010000000000000000000000000001010000000010010010001010100100010011",
		B"000000000000000000000000001000010001010011100010010010001010100100100000",
		B"000000000000000000000000001000010001010011100010010010001010100100010000",
		B"000000000000000000000000001000010001010011100010010010001010100100100001",
		B"000000000000000000000000001000010001010011100010010010001010100100010001",
		B"000000000000000000000000001000010001010011100010010010001010100100100010",
		B"000000000000000000000000001000010001010011100010010010001010100100010010",
		B"000000000000000000000000001000010001010011100010010010001010100100100011",
		B"000000000000000000000000001000010001010011100010010010001010100100010011",
		B"000000000000000000000000010100010001010001100010010010001010100100010000",
		B"000000000000000000000000010100010001010001000010010010001010100100010000",
		B"000000000000000000000000010100010001010010000010010010001010100100010000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000001100010001010010100010010010001010100100000000",
		B"000000000000000000000000001100010011010010100010010010001010100100000000",
		B"000000000000000000000000000000000001010001000000010001001000000000000000",
		B"000000000000000000000000001100010101010010100010010010001010100100000000",
		B"000000000000000000000000001100010001010000000010010010101010100100000000",
		B"000000000000000000110001000000000001010000000010010010101010100100000001",
		B"000000000011000100000000000000000001010000000010010010101010100100000010",
		B"001100010000000000000000000000000001010000000010010010101010100100000011",
		B"000000000000000000000000000000100001010001100000010000001000000000000000",
		B"000000000000000000000010000000000001010001100000010000001000000000000001",
		B"000000000000001000000000000000000001010001100000010000001000000000000010",
		B"000000100000000000000000000000000001010001100000010000001000000000000011",
		B"000000000000000000000000000001000001010001100000010000001000000000000000",
		B"000000000000000000000100000000000001010001100000010000001000000000000001",
		B"000000000000010000000000000000000001010001100000010000001000000000000010",
		B"000001000000000000000000000000000001010001100000010000001000000000000011",
		B"000000000000000000000000101100010001010100000000010001001000000000000000",
		B"000000000000000010110001001100010001010100000000010001001000000000000001",
		B"000000001011000100000000001100010001010100000000010001001000000000000010",
		B"101100010000000000000000001100010001010100000000010001001000000000000011",
		B"000010000000100000001000000010000001010001000000010001001000000000000000",
		B"000000000000000000000000001000010001010011100000010001001000000000000100",
		B"000000000000000000000000001000010001010011100000010001001000000000001000",
		B"000000000000000000000000001000010001010011100000010001001000000000001100",
		B"000000000000000000000000001100010001010100000000010001001000000000000000",
		B"000000000000000000110001000000000001010100000000010001001000000000000000",
		B"000000000011000100000000000000000001010100000000010001001000000000000000",
		B"001100010000000000000000000000000001010100000000010001001000000000000000",
		B"000000000000000000000000001000010001010011100000010000001000000000000000",
		B"000000000000000000000000001000010001010011100000010000001000000000000001",
		B"000000000000000000000000001000010001010011100000010000001000000000000010",
		B"000000000000000000000000001000010001010011100000010000001000000000000011",
		B"000000000000000000000000001100010001010100000000010001001000000000000000",
		B"000000000000000000000000001100010001010100000000010001001000000000000001",
		B"000000000000000000000000001100010001010100000000010001001000000000000010",
		B"000000000000000000000000001100010001010100000000010001001000000000000011",
		B"000000000000000000000000000010000001010001100000010000001000000000000000",
		B"000000000000000000001000000000000001010001100000010000001000000000000001",
		B"000000000000100000000000000000000001010001100000010000001000000000000010",
		B"000010000000000000000000000000000001010001100000010000001000000000000011",
		B"000000000000000000000000001100010001010010100000010001001000000010000000",
		B"000000000000000000000000001100010001010010100000010001001000000001000000",
		B"000000000000000000000000001100010001010010100000010001001000000010000100",
		B"000000000000000000000000001100010001010010100000010001001000000001000100",
		B"000000000000000000000000001100010001010010100000010001001000000010001000",
		B"000000000000000000000000001100010001010010100000010001001000000001001000",
		B"000000000000000000000000001100010001010010100000010001001000000010001100",
		B"000000000000000000000000001100010001010010100000010001001000000001001100",
		B"000000000000000000000000001100010011010010100000010001001000000010000000",
		B"000000000000000000000000001100010011010010100000010001001000000001000000",
		B"000000000000000000000000001100010011010010100000010001001000000010000100",
		B"000000000000000000000000001100010011010010100000010001001000000001000100",
		B"000000000000000000000000001100010011010010100000010001001000000010001000",
		B"000000000000000000000000001100010011010010100000010001001000000001001000",
		B"000000000000000000000000001100010011010010100000010001001000000010001100",
		B"000000000000000000000000001100010011010010100000010001001000000001001100",
		B"000000000000000000000000001100010001010000100000010001000000000000000000",
		B"000000000000000000000000001100010001010000100000010001001000000000000100",
		B"000000000000000000000000001100010001010000100000010001001000000000001000",
		B"000000000000000000000000001100010001010000100000010001001000000000001100",
		B"000000000000000000000000001100010001010000000000010001001000000000000000",
		B"000000000000000000000000001100010001010000000000010001001000000000000100",
		B"000000000000000000000000001100010001010000000000010001001000000000001000",
		B"000000000000000000000000001100010001010000000000010001001000000000001100",
		B"000000100000001000000010000000100001010001000000010001001000000000000000",
		B"000001000000010000000100000001000001010001000000010001001000000000000000",
		B"000000000000000000000000001100010101010010100000010001001000000010000100",
		B"000000000000000000000000001100010101010010100000010001001000000001000100",
		B"000000000000000000000000001100010101010010100000010001001000000010001000",
		B"000000000000000000000000001100010101010010100000010001001000000001001000",
		B"000000000000000000000000001100010101010010100000010001001000000010001100",
		B"000000000000000000000000001100010101010010100000010001001000000001001100"
	)

	private val signalMem = Mem(content)

	io.controlSignals := signalMem.readSync(io.opcode.asUInt, io.strobe).as(PipelineControl())
}

