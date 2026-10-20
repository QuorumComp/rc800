package rc800.decoder

import spinal.core._
import spinal.core.sim._

import java.io._


/**
 * Simulation tops for decoder regression testing.
 *
 * OpcodeSimTop exposes the combinational OpcodeDecoder only — it is the source
 * of the golden control-word table (decoder-golden.txt).  Its outputs are
 * registered so the top level has a clock for simulators that drive one.
 *
 * PairSimTop additionally instantiates the ROM-based LookupDecoder so the two
 * implementations can be cross-checked against each other.
 */
class OpcodeSimTop extends Component {
	val io = new Bundle {
		val opcode = in  Bits(8 bits)
		val word   = out UInt(DecoderOutput().asBits.getWidth bits)
	}

	private val opcodeDecoder = OpcodeDecoder()
	opcodeDecoder.io.opcode := io.opcode
	io.word := RegNext(opcodeDecoder.io.output.asBits.asUInt)

	// Captured at elaboration: named, flattened leaves of the decoder output,
	// in hardware bit order, with enum element names for decoding values.
	val layout: Seq[DecoderGoldenGen.LeafInfo] =
		DecoderGoldenGen.captureLayout(opcodeDecoder.io.output, "out")
}


class PairSimTop extends Component {
	val io = new Bundle {
		val opcode     = in  Bits(8 bits)
		val combWord   = out UInt(DecoderOutput().asBits.getWidth bits)
		val lookupWord = out UInt(DecoderOutput().asBits.getWidth bits)
	}

	private val opcodeDecoder = OpcodeDecoder()
	opcodeDecoder.io.opcode := io.opcode
	io.combWord := opcodeDecoder.io.output.asBits.asUInt

	private val lookupDecoder = LookupDecoder()
	lookupDecoder.io.opcode := io.opcode
	lookupDecoder.io.strobe := True
	io.lookupWord := lookupDecoder.io.output.asBits.asUInt
}


object DecoderGoldenGen {
	val simConfig = SimConfig

	case class LeafInfo(path: String, leaf: BaseType) {
		def width: Int = leaf.getBitsWidth

		// Enum element names come from the explicit tables below: SpinalHDL's
		// internal element naming is unreliable outside the naming phase
		// (empty or alias-suffixed names depending on when it is queried).
		lazy val enumNames: Seq[String] = leaf match {
			case craft: SpinalEnumCraft[_] => enumNameTables.collectFirst {
				case (factory, names) if craft.spinalEnum eq factory =>
					assert(names.size == craft.spinalEnum.elements.size,
						s"enum name table out of date for ${factory.getClass.getName}: " +
						s"table has ${names.size}, enum has ${craft.spinalEnum.elements.size}")
					names
			}.getOrElse(throw new AssertionError(
				s"no name table for enum ${craft.spinalEnum.getClass.getName}"))
			case _ => Seq.empty[String]
		}

		def render(value: BigInt): String =
			if (enumNames.nonEmpty && value >= 0 && value < enumNames.size) enumNames(value.toInt)
			else if (enumNames.nonEmpty) s"INVALID($value)"
			else value.toString
	}

	// Element names in declaration order (= binarySequential position order).
	// The assert in enumNames catches any element added to the real enums.
	val enumNameTables: Seq[(SpinalEnum, Seq[String])] = Seq(
		rc800.registers.RegisterName -> Seq("f", "t", "b", "c", "d", "e", "h", "l", "ft", "bc", "de", "hl"),
		rc800.alu.OperandSource -> Seq("zero", "ones", "register", "pc", "memory", "signed_memory"),
		rc800.alu.PcCondition -> Seq("always", "whenConditionMet", "whenResultNotZero"),
		rc800.alu.PcTruePathSource -> Seq("offsetFromMemory", "offsetFromDecoder", "register2", "vectorFromMemory", "vectorFromDecoder"),
		rc800.alu.AluOperation -> Seq("add", "sub", "compare", "extend1", "and", "or", "xor", "operand1", "ls", "rs", "rsa", "swap"),
		rc800.alu.Condition -> Seq("le", "gt", "lt", "ge", "leu", "gtu", "ltu", "geu", "eq", "ne", "t", "f"),
		rc800.control.MemoryStageAddressSource -> Seq("register1", "pc"),
		rc800.control.WriteBackValueSource -> Seq("alu", "memory"))

	def captureLayout(data: Data, path: String): Seq[LeafInfo] = data match {
		case bundle: Bundle =>
			bundle.elements.toSeq.flatMap { case (name, element) =>
				captureLayout(element, s"$path.$name")
			}
		case vector: Vec[_] =>
			(0 until vector.size).flatMap(i => captureLayout(vector(i), s"$path($i)"))
		case leaf: BaseType =>
			Seq(LeafInfo(path, leaf))
	}

	/**
	 * Runs one simulation of the combinational decoder over all 256 opcodes.
	 * Shared between the generator main and the regression tests so the
	 * simulation only runs once per JVM.
	 */
	case class SimResult(
		words: Seq[BigInt],
		lines: Seq[String],
		layoutPaths: Seq[String],
		layoutWidths: Seq[Int]) {

		def hasInvalidEnumValues: Seq[String] =
			lines.filter(_.contains("INVALID(")).map(_.takeWhile(_ != ' '))
	}

	lazy val simResult: SimResult = {
		var words: Vector[BigInt] = Vector.empty
		var layout: Seq[LeafInfo] = null

		simConfig.compile(new OpcodeSimTop).doSim { dut =>
			layout = dut.layout
			dut.clockDomain.forkStimulus(period = 10)
			for (op <- 0 until 256) {
				dut.io.opcode #= op
				dut.clockDomain.waitRisingEdge()
				dut.clockDomain.waitRisingEdge()   // registered output valid
				words :+= dut.io.word.toBigInt
			}
		}

		// Render each control word by slicing the raw as-bits value in layout
		// order, LSB-first (bundle elements are laid out first-declared-lowest,
		// verified against the known-good NOP defaults).
		val lines = words.zipWithIndex.map { case (word, op) =>
			var rest = word
			val values = layout.map { info =>
				val value = rest & ((BigInt(1) << info.width) - 1)
				rest = rest >> info.width
				info.render(value)
			}
			f"${"%02X".format(op)} ${OpcodeMap.label(op)}%-16s " + values.mkString(" ")
		}

		SimResult(words, lines, layout.map(_.path), layout.map(_.width))
	}

	/**
	 * Runs one simulation pairing OpcodeDecoder against LookupDecoder.
	 * Only valid once the ROM table elaborates (its row width must match
	 * DecoderOutput).
	 */
	lazy val lookupWords: Seq[BigInt] = {
		var words: Vector[BigInt] = Vector.empty
		simConfig.compile(new PairSimTop).doSim { dut =>
			dut.clockDomain.forkStimulus(period = 10)
			for (op <- 0 until 256) {
				dut.io.opcode #= op
				dut.clockDomain.waitRisingEdge()   // ROM readSync samples the address
				dut.clockDomain.waitRisingEdge()   // ROM data + combinational outputs valid
				words :+= dut.io.lookupWord.toBigInt
			}
		}
		words
	}

	def lookupMismatches: Seq[Int] =
		(0 until 256).filter(op => simResult.words(op) != lookupWords(op))

	def header: Seq[String] = Seq(
		"# RC811 OpcodeDecoder golden control words.",
		"# Generated by: sbt \"runMain rc800.decoder.DecoderGoldenGen\"  (then commit the diff).",
		"# Checked by:   sbt test  (DecoderGoldenTest regenerates and diffs).",
		"# One row per opcode: <hex> <mnemonic> <field values in layout order>.",
		"# Enums are rendered by element name (INVALID(n) = value out of range),",
		"# bools as 0/1, other values as unsigned decimal.",
		"# layout: " + simResult.layoutPaths.mkString(","),
		"# widths: " + simResult.layoutWidths.mkString(","))

	def goldenText: String = (header ++ simResult.lines).mkString("\n") + "\n"

	def defaultPath: String = {
		val candidates = Seq(
			"rc800/spinal/decoder-golden.txt",
			"spinal/decoder-golden.txt",
			"decoder-golden.txt")
		candidates.find(new File(_).exists).getOrElse(candidates.head)
	}

	def main(args: Array[String]): Unit = {
		val path = args.headOption.getOrElse(defaultPath)
		val writer = new PrintWriter(new File(path))
		try writer.print(goldenText) finally writer.close()
		println(s"wrote $path (${simResult.lines.size} opcodes)")
	}
}
