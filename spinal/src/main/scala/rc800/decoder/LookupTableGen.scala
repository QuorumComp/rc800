package rc800.decoder


/**
 * Regenerates LookupDecoder.scala from the control words produced by the
 * combinational OpcodeDecoder (via DecoderGoldenGen.simResult).
 *
 * The ROM table has no independent source of truth: whenever PipelineControl
 * or the decode map changes, run this and commit the result.  DecoderLookupTest
 * fails if the committed table and the decoder ever diverge again.
 */
object LookupTableGen {
	val targetPath: String = {
		val candidates = Seq(
			"rc800/spinal/src/main/scala/rc800/decoder/LookupDecoder.scala",
			"spinal/src/main/scala/rc800/decoder/LookupDecoder.scala",
			"src/main/scala/rc800/decoder/LookupDecoder.scala")
		candidates.find(p => new java.io.File(p).exists).getOrElse(candidates.head)
	}

	def main(args: Array[String]): Unit = {
		val words = DecoderGoldenGen.simResult.words
		val width = DecoderGoldenGen.simResult.layoutWidths.sum
		assert(words.map(w => w >= 0 && w < (BigInt(1) << width)).reduce(_ && _),
			"control word exceeds layout width")

		def binary(w: BigInt): String = {
			val s = w.toString(2)
			"0" * (width - s.length) + s
		}

		val rows = words.zipWithIndex.map { case (w, op) =>
			f"""		B"${binary(w)}", // $op%02X ${OpcodeMap.label(op)}"""
		}

		val source =
			s"""package rc800.decoder
				|
				|import spinal.core._
				|
				|import rc800.alu.Condition
				|import rc800.control.PipelineControl
				|import rc800.control.AluStageControl
				|
				|
				|/**
				| * ROM decoder: one $width-bit control word per opcode.
				| *
				| * GENERATED FILE — do not edit by hand.
				| * Regenerate with: sbt "runMain rc800.decoder.LookupTableGen"
				| * Field legend: rc800/spinal/decoder-golden.txt (layout header).
				| */
				|case class LookupDecoder() extends Component {
				|\tval io = new Bundle {
				|\t\tval opcode = in Bits(8 bits)
				|\t\tval strobe = in Bool()
				|\t\tval nmiActive = in Bool()
				|\t\tval output = out (DecoderOutput())
				|
				|\t\tdef controlSignals = output.stageControl
				|\t}
				|
				|\tprivate val signalMem = Mem(Array(
				|${rows.mkString("\n")}
				|\t))
				|
				|	// Synchronous read: the table is clocked by the pipeline
				|	// strobe, so its own output register is the stage register
				|	// that holds the control word for the window executing the
				|	// instruction, and the pipeline areas read it directly
				|	// instead of each carrying a duplicate flop.  (This does
				|	// not make the table infer as block RAM: the $$readmemb
				|	// initialisation is not ROM content for the mapper.  See
				|	// BRAM-DECODE-PLAN.md.)
				|\tio.output := signalMem.readSync(io.opcode.asUInt, io.strobe).as(DecoderOutput())
				|	// NMI must not interrupt NMI: the fault flags drop while
				|	// NMI mode is active.  ROM words are captured with the
				|	// faults inactive, so only the flags need masking.
				|\twhen (io.nmiActive) {
				|\t\tio.output.illegal := False
				|\t\tio.output.overflow := False
				|\t}
				|}
				|""".stripMargin

		val path = args.headOption.getOrElse(targetPath)
		writeIfChanged(new java.io.File(path), source)
		println(s"$path: $status ($width bits x 256 rows)")
	}

	// Writing unconditionally bumps the file's mtime, which makes every
	// downstream build stage - including the board Verilog, whose inputs are
	// the Scala sources - look stale after a regeneration that changed
	// nothing.  Generators run on every RTL build now, so they must be
	// idempotent in both content and timestamp.
	private var status = "regenerated"

	private def writeIfChanged(file: java.io.File, text: String): Unit = {
		if (file.exists) {
			val onDisk = new String(java.nio.file.Files.readAllBytes(file.toPath)).replace("\r\n", "\n")
			if (onDisk == text.replace("\r\n", "\n")) { status = "already up to date"; return }
		}
		val writer = new java.io.PrintWriter(file)
		try writer.print(text) finally writer.close()
	}
}
