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

	test("chain order in OpcodeDecoder.scala matches OpcodeMap.entries") {
		// Works both from the submodule's own build (cwd spinal/) and from the
		// parent hc800 build (cwd rtl/).
		val source = Seq(
			"src/main/scala/rc800/decoder/OpcodeDecoder.scala",
			"rc800/spinal/src/main/scala/rc800/decoder/OpcodeDecoder.scala",
			"spinal/src/main/scala/rc800/decoder/OpcodeDecoder.scala")
			.map(new File(_))
			.find(_.exists)
			.getOrElse(new File("src/main/scala/rc800/decoder/OpcodeDecoder.scala"))
		assert(source.exists(), s"decoder source not found at ${source.getAbsolutePath}")
		val text = new String(Files.readAllBytes(source.toPath))
		val chainMatches = "(?:when|elsewhen)\\s*\\(\\s*io\\.opcode\\s*===\\s*Opcodes\\.([A-Z0-9_]+)".r
			.findAllMatchIn(text)
			.map(_.group(1))
			.toList
		assertEquals(chainMatches, OpcodeMap.entries.map(_.name).toList,
			"the when/elsewhen chain and OpcodeMap.entries drifted apart")
	}

	test("ambiguous opcode matches are only exact opcodes shadowing field forms") {
		// Encoding technique: no-field instructions (exact 8-bit masks) live in
		// the don't-care holes of wider field forms tested later in the chain
		// (e.g. NOP 0x00 inside LD MEM,T "000000--", J_CC_I under SYS_I).  Any
		// other overlap shape would be an encoding accident.
		for (op <- 0 until 256) {
			val matches = OpcodeMap.allMatches(op)
			if (matches.size > 1) {
				assertEquals(OpcodeMap.resolve(op), matches.head,
					f"opcode $op%02X should resolve to the earliest chain match")
				val winner = OpcodeMap.entries.find(_.name == matches.head).get
				assertEquals(winner.mask.careAbout, BigInt(0xFF),
					f"opcode $op%02X is ambiguous between $matches but the winner is not an exact mask")
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
