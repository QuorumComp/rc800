package rc800.decoder

import spinal.core._


/**
 * Opcode decode map: the single Scala-level source of truth for which opcode
 * matches which instruction, mirroring the when/elsewhen chain in
 * OpcodeDecoder.scala.  Illegal opcodes take priority over the chain, and the
 * chain is matched in order (first match wins).  OpcodeAuditTest checks that
 * this map agrees with the actual decoder source.
 */
case class DecodeEntry(name: String, mask: MaskedLiteral) {
	def matches(opcode: Int): Boolean =
		((BigInt(opcode) ^ mask.value) & mask.careAbout) == 0

	/** Assembly-ish label derived from the constant name: ADD_T_R8 -> "ADD T,R8". */
	def mnemonic: String = name.replaceFirst("_", " ").replace('_', ',')
}


object OpcodeMap {
	/**
	 * In the order the when/elsewhen chain tests them.  J_CC_I (1001----)
	 * deliberately overlaps SYS_I and the illegal 100111-- opcodes; the chain
	 * order and the leading illegal check resolve the overlap.
	 */
	val entries: Seq[DecodeEntry] = Seq(
		// Opcodes with no fields
		DecodeEntry("AND_T_I",   Opcodes.AND_T_I),
		DecodeEntry("DI",        Opcodes.DI),
		DecodeEntry("EI",        Opcodes.EI),
		DecodeEntry("EXT_T",     Opcodes.EXT_T),
		DecodeEntry("LD_CR_T",   Opcodes.LD_CR_T),
		DecodeEntry("LD_T_CR",   Opcodes.LD_T_CR),
		DecodeEntry("LS_FT_I",   Opcodes.LS_FT_I),
		DecodeEntry("NEG_T",     Opcodes.NEG_T),
		DecodeEntry("NEG_FT",    Opcodes.NEG_FT),
		DecodeEntry("NOP",       Opcodes.NOP),
		DecodeEntry("NOT_F",     Opcodes.NOT_F),
		DecodeEntry("OR_T_I",    Opcodes.OR_T_I),
		DecodeEntry("POPA",      Opcodes.POPA),
		DecodeEntry("PUSHA",     Opcodes.PUSHA),
		DecodeEntry("RETI",      Opcodes.RETI),
		DecodeEntry("RS_FT_I",   Opcodes.RS_FT_I),
		DecodeEntry("RSA_FT_I",  Opcodes.RSA_FT_I),
		DecodeEntry("SWAPA",     Opcodes.SWAPA),
		DecodeEntry("SYS_I",     Opcodes.SYS_I),
		DecodeEntry("XOR_T_I",   Opcodes.XOR_T_I),
		// Opcodes with two-bit field
		DecodeEntry("ADD_FT_R16", Opcodes.ADD_FT_R16),
		DecodeEntry("ADD_R16_I",  Opcodes.ADD_R16_I),
		DecodeEntry("CMP_FT_R16", Opcodes.CMP_FT_R16),
		DecodeEntry("EXG_FT_R16", Opcodes.EXG_FT_R16),
		DecodeEntry("JAL_R16",    Opcodes.JAL_R16),
		DecodeEntry("J_R16",      Opcodes.J_R16),
		DecodeEntry("LD_FT_R16",  Opcodes.LD_FT_R16),
		DecodeEntry("LD_IO_T",    Opcodes.LD_IO_T),
		DecodeEntry("LD_MEM_T",   Opcodes.LD_MEM_T),
		DecodeEntry("LD_R16_FT",  Opcodes.LD_R16_FT),
		DecodeEntry("LD_T_IO",    Opcodes.LD_T_IO),
		DecodeEntry("LD_T_CODE",  Opcodes.LD_T_CODE),
		DecodeEntry("LD_T_MEM",   Opcodes.LD_T_MEM),
		DecodeEntry("PICK",       Opcodes.PICK),
		DecodeEntry("POP",        Opcodes.POP),
		DecodeEntry("PUSH",       Opcodes.PUSH),
		DecodeEntry("STOW",       Opcodes.STOW),
		DecodeEntry("SUB_FT_R16", Opcodes.SUB_FT_R16),
		DecodeEntry("SWAP",       Opcodes.SWAP),
		DecodeEntry("TST_R16",    Opcodes.TST_R16),
		// Opcodes with three-bit field
		DecodeEntry("ADD_T_R8",  Opcodes.ADD_T_R8),
		DecodeEntry("ADD_R8_I",  Opcodes.ADD_R8_I),
		DecodeEntry("AND_T_R8",  Opcodes.AND_T_R8),
		DecodeEntry("CMP_R8_I",  Opcodes.CMP_R8_I),
		DecodeEntry("CMP_T_R8",  Opcodes.CMP_T_R8),
		DecodeEntry("DJ_R8_I",   Opcodes.DJ_R8_I),
		DecodeEntry("EXG_T_R8",  Opcodes.EXG_T_R8),
		DecodeEntry("LD_MEM_R8", Opcodes.LD_MEM_R8),
		DecodeEntry("LD_R8_I",   Opcodes.LD_R8_I),
		DecodeEntry("LD_R8_MEM", Opcodes.LD_R8_MEM),
		DecodeEntry("LD_R8_T",   Opcodes.LD_R8_T),
		DecodeEntry("LD_T_R8",   Opcodes.LD_T_R8),
		DecodeEntry("LS_FT_R8",  Opcodes.LS_FT_R8),
		DecodeEntry("OR_T_R8",   Opcodes.OR_T_R8),
		DecodeEntry("RS_FT_R8",  Opcodes.RS_FT_R8),
		DecodeEntry("RSA_FT_R8", Opcodes.RSA_FT_R8),
		DecodeEntry("SUB_T_R8",  Opcodes.SUB_T_R8),
		DecodeEntry("XOR_T_R8",  Opcodes.XOR_T_R8),
		// Opcodes with four-bit field
		DecodeEntry("J_CC_I",    Opcodes.J_CC_I)
	)

	def illegals: Seq[MaskedLiteral] = Opcodes.illegals

	/** Names of the chain entries that also match this opcode, chain order. */
	def allMatches(opcode: Int): Seq[String] =
		entries.filter(_.matches(opcode)).map(_.name)

	/**
	 * The entry the decoder actually selects for this opcode: illegal wins,
	 * otherwise the first matching chain entry.  Every opcode must resolve.
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
