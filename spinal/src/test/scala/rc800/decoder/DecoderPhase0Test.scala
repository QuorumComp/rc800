package rc800.decoder

import java.io.File
import java.nio.file.Files

/**
 * Phase 0 regression tests for the RC811 instruction decoder.
 *
 *  - DecoderGoldenTest: the committed golden control-word table must match a
 *    fresh simulation of the decoder (run the generator and commit the diff
 *    when the decoder changes on purpose).
 *  - DecoderLookupEquivalenceTest: the ROM-based LookupDecoder must produce
 *    bit-identical control words to the combinational OpcodeDecoder.
 *  - OpcodeAuditTest: the Scala-level OpcodeMap must agree with the actual
 *    when/elsewhen chain in OpcodeDecoder.scala, every opcode must resolve,
 *    and opcode overlaps must keep their documented shape (exact no-field
 *    opcodes shadowing wider field forms; J_CC_I only on valid conditions).
 */
class DecoderGoldenTest extends munit.FunSuite {

	test("golden control-word table matches a fresh decoder simulation") {
		val file = new File(DecoderGoldenGen.defaultPath)
		assert(file.exists(),
			s"golden file missing at ${file.getAbsolutePath}: generate it with " +
			"sbt \"runMain rc800.decoder.DecoderGoldenGen\" and commit it")

		val committed = new String(Files.readAllBytes(file.toPath)).replace("\r\n", "\n")
		val fresh = DecoderGoldenGen.goldenText

		if (committed != fresh) {
			val committedLines = committed.split("\n", -1)
			val freshLines = fresh.split("\n", -1)
			val firstDiff = (0 until math.min(committedLines.length, freshLines.length))
				.iterator.find(i => committedLines(i) != freshLines(i))
				.getOrElse(math.min(committedLines.length, freshLines.length))
			val clue =
				if (firstDiff < math.min(committedLines.length, freshLines.length))
					f"\nfirst differing line ${firstDiff + 1}:\n" +
					f"  committed: ${committedLines(firstDiff)}\n  generated: ${freshLines(firstDiff)}"
				else
					s"\nline count differs: committed=${committedLines.length} generated=${freshLines.length}"
			fail("decoder-golden.txt is out of date:" + clue)
		}
	}

	test("no opcode decodes to an out-of-range enum value") {
		assertEquals(DecoderGoldenGen.simResult.hasInvalidEnumValues, Nil,
			"these opcodes drive an enum field outside its element range")
	}

	test("NOP is the documented baseline: no memory access, no register write, PC +1") {
		// The decoder lays the NOP control word down before any effect runs, so
		// every instruction inherits these; if an effect ever needs a different
		// baseline this pins what that baseline currently is.
		val paths = DecoderGoldenGen.simResult.layoutPaths
		val nopValues = DecoderGoldenGen.simResult.lines.head.substring(20).split(" ")
		def field(path: String): String = {
			val i = paths.indexOf(path)
			assert(i >= 0, s"no such control field: $path")
			nopValues(i)
		}
		assertEquals(field("out.stageControl.memoryStageControl.enable"), "0")
		assertEquals(field("out.stageControl.memoryStageControl.write"), "0")
		assertEquals(field("out.stageControl.writeStageControl.fileControl.write"), "0")
		assertEquals(field("out.stageControl.writeStageControl.fileControl.writeAux"), "0")
		assertEquals(field("out.stageControl.aluStageControl.pcControl.condition"), "always")
		assertEquals(field("out.stageControl.aluStageControl.pcControl.truePath"), "offsetFromDecoder")
	}
}


class DecoderLookupEquivalenceTest extends munit.FunSuite {

	test("LookupDecoder agrees with OpcodeDecoder on all 256 opcodes") {
		val mismatches = DecoderGoldenGen.lookupMismatches
		assertEquals(mismatches, Nil,
			"opcodes where the ROM table disagrees with the chain (regenerate " +
			"LookupDecoder.scala with sbt \"runMain rc800.decoder.LookupTableGen\")")
	}
}


class OpcodeAuditTest extends munit.FunSuite {

	test("every opcode resolves to exactly one decode entry") {
		for (op <- 0 until 256) {
			OpcodeMap.resolve(op)   // throws if nothing matches
		}
	}

	test("ambiguous opcode matches are only winners shadowing the other forms") {
		// Encoding technique: a form wins over another form matching the same
		// opcode only by being strictly more specific - an exact mask inside a
		// field form (NOP 0x00 inside LD MEM,T "000000--", SYS_I under J_CC_I),
		// or a narrower field inside a wider one (ADD_R16_I "100111--" under
		// J_CC_I "1001----").  Any overlap the chain resolves without the
		// winner's care bits including the loser's would be an encoding
		// accident.
		for (op <- 0 until 256) {
			val matches = OpcodeMap.allMatches(op)
			if (matches.size > 1) {
				assertEquals(OpcodeMap.resolve(op), matches.head,
					f"opcode $op%02X should resolve to the earliest chain match")
				val winner = OpcodeMap.entries.find(_.name == matches.head).get
				for (loser <- matches.tail) {
					val loserMask = OpcodeMap.entries.find(_.name == loser).get.mask.careAbout
					assertEquals((winner.mask.careAbout & loserMask), loserMask,
						f"opcode $op%02X is ambiguous between $matches but the winner does not shadow $loser")
				}
			}
		}
	}

	test("J_CC_I only fires on opcodes whose cc field is a valid Condition") {
		val conditionCount = rc800.alu.Condition.elements.size
		for (op <- 0 until 256 if OpcodeMap.resolve(op) == "J_CC_I") {
			val cc = op & 0xF
			assert(cc < conditionCount,
				f"opcode $op%02X reaches J_CC_I with cc=$cc, outside Condition(0..${conditionCount - 1})")
		}
	}
}
