package rc800.decoder

import spinal.core._


/**
 * Opcode decode map: the Scala-level view used for audit and for labelling the
 * golden table.  It is a derived view of the decoder's own `instructions` table
 * rather than a hand-maintained parallel list - `entries` is just
 * `OpcodeDecoder.instructions` projected into `DecodeEntry`.  Illegal opcodes
 * take priority over the table, and the table is matched in order (first match
 * wins), exactly as the decoder dispatches.
 */
case class DecodeEntry(name: String, mask: MaskedLiteral) {
	def matches(opcode: Int): Boolean =
		((BigInt(opcode) ^ mask.value) & mask.careAbout) == 0

	/** Assembly-ish label derived from the constant name: ADD_T_R8 -> "ADD T,R8". */
	def mnemonic: String = name.replaceFirst("_", " ").replace('_', ',')
}


object OpcodeMap {
	/**
	 * In the order the decoder tests them.  J_CC_I (1001----) deliberately
	 * overlaps SYS_I and ADD_R16_I (100111--); the order resolves both overlaps
	 * (the more specific form is tested first).  Derived from the decoder's own
	 * `instructions` table.
	 */
	val entries: Seq[DecodeEntry] =
		OpcodeDecoder.instructions.map(i => DecodeEntry(i.name, i.mask))

	def illegals: Seq[MaskedLiteral] = Opcodes.illegals

	/** Names of the table rows that also match this opcode, priority order. */
	def allMatches(opcode: Int): Seq[String] =
		entries.filter(_.matches(opcode)).map(_.name)

	/**
	 * The entry the decoder actually selects for this opcode: illegal wins,
	 * otherwise the first matching table row.  Every opcode must resolve.
	 */
	def resolve(opcode: Int): String = {
		if (illegals.exists(m => ((BigInt(opcode) ^ m.value) & m.careAbout) == 0)) "ILLEGAL"
		else entries.find(_.matches(opcode)).map(_.name)
			.getOrElse(throw new AssertionError(f"opcode $opcode%02X matches no decode entry"))
	}

	/** Assembly-ish label for the golden file. */
	def label(opcode: Int): String = resolve(opcode) match {
		case "ILLEGAL" => "ILLEGAL"
		case name      => entries.iterator.find(_.name == name).map(_.mnemonic).get
	}
}
