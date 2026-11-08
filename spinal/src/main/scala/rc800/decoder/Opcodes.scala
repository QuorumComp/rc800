package rc800.decoder

import spinal.core._

object Opcodes {
	def illegals = List(
		M"00010100",		// $14: LD T,(FT+)
		M"00011000",		// $18: LD (FT+),F
		M"00011100",		// $1C: LD F,(FT+)
		M"00000001",		// $01: LD (BC),T
		M"0000001-",		// $02 $03: LD (DE),T / LD (HL),T
		M"00000100",		// $04: LD T,(FT) - the plain-access form is gone; it lives at $29
		M"00000101",		// $05: LD T,(BC)
		M"0000011-",		// $06 $07: LD T,(DE) / LD T,(HL)
		M"00001000",		// $08: illegal (LD F,(FT) is $28)
		M"00001001",		// $09: illegal (LD T,(FT) is $29)
		M"0010000-",		// $20 $21: LD (FT),F / LD (FT),T - free cells
		M"01110001",
		M"01111001",
		M"10110011",
		M"10111011",		// $BB: hole; RSA FT,i8 lives at $B9
		M"11001000",
		M"11010000",
		M"11011000",
		M"1110000-",		// $E0 $E1: RS FT,F / RS FT,T — shifts never take the hub bytes
		M"1111000-",		// $F0 $F1: LS FT,F / LS FT,T
		M"11111100",		// $FC: ADD FT,FT has no encoding; NEG FT owns $F8
	)

	def NOP_opcode = 0x00

	def AND_T_I    = M"10110001"
	def DI         = M"01100001"		// under RETI since the ft-step swap; was $09
	def EI         = M"01101001"		// under DI since the ft-step swap; was $08
	def EXT_T      = M"01001001"
	def LD_CR_T    = M"00001010"
	def LD_F_MEM   = M"00101000"		// LD F,(FT): the non-stepping hub-byte load, $28 (the r=0 exception cell on the LD r8,(FT+) home row)
	def LD_T_CR    = M"00001011"
	def LD_T_MEM   = M"00101001"		// LD T,(FT): the non-stepping hub-byte load, $29 (the r=1 exception cell on the LD r8,(FT+) home row)
	def LS_FT_I    = M"10111010"
	def NEG_T      = M"01010001"
	def NEG_FT     = M"11111000"
	def NOP        = M"00000000"
	def NOT_F      = M"00110000"
	def OR_T_I     = M"10110000"
	def POPA       = M"11101001"
	def PUSHA      = M"11101000"
	def RETI       = M"01011001"
	def RS_FT_I    = M"10111000"
	def RSA_FT_I   = M"10111001"
	def SWAPA      = M"11001100"
	def SYS_I      = M"10011011"
	def XOR_T_I    = M"10110010"

	def ADD_FT_R16 = M"111111--"
	def ADD_R16_I  = M"100111--"		// Add signed immediate to register pair
	def CMP_FT_R16 = M"110011--"
	def EXG_FT_R16 = M"110010--"
	def JAL_R16    = M"001110--"
	def J_R16      = M"001111--"
	def LD_FT_R16  = M"110110--"		// Load FT with register pair
	def LD_IO_T    = M"001100--"		// Load I/O with T, then step the pair ($31-$33; $30 is NOT F, tested first). Stepping form since the lco-step swap.
	def LD_R16_FT  = M"110100--"		// Load register pair with FT
	def LD_T_CODE  = M"00001100"		// LD T,(FT) through code space: the non-stepping exception cell ($0C) of the stepping LD T,CODE row below
	def LD_T_CODE_INC = M"000011--"	// LCO T,(R16), then step R16 up ($0D-$0F; $0C stays the non-stepping FT exception, claimed by LD_T_CODE above)
	def LD_T_IO    = M"00110100"		// Load T with I/O: the non-stepping exception cell ($34) of the stepping LD T,IO row below
	def LD_T_IO_INC = M"001101--"		// LIO T,(R16), then step R16 up ($35-$37; $34 stays the non-stepping FT exception, claimed by LD_T_IO above)
	def PICK_I     = M"101111--"		// Pick stack entry at immediate index
	def POP        = M"110001--"
	def PUSH       = M"110000--"
	def STOW_I     = M"101101--"		// Store register pair at immediate index
	def SUB_FT_R16 = M"111110--"
	def SWAP       = M"110111--"
	def TST_R16    = M"110101--"

	def ADD_T_R8   = M"01000---"
	def ADD_R8_I   = M"10100---"
	def AND_T_R8   = M"01101---"
	def CMP_R8_I   = M"10101---"
	def CMP_T_R8   = M"01001---"
	def DJ_R8_I    = M"10001---"
	def EXG_T_F    = M"00010000"		// EXG T,F; the only byte exchange ($10; doubles as the illegal LD (FT+),T)
	def LD_MEM_T_INC = M"000100--"		// Store T to (R16), then step R16 up ($10-$13; $10 is EXG)
	def LD_MEM_F_INC = M"000110--"		// Store F to (R16), then step R16 up ($18-$1B; $18 is the illegal FT pointer)
	def LD_MEM_R8  = M"001001--"		// LD (FT+),r8: stores through the hub pointer and step it, $24-$27 (the r=0,1 F,T cells are illegal, the r=2,3 B,C half is LD_MEM_BC)
	def LD_MEM_BC  = M"0010001-"	// LD (FT+),B / LD (FT+),C: the r=2,3 half of the stepping store row, $22 $23
	def LD_R8_I    = M"10000---"		// Load 8 bit immediate
	def LD_R8_MEM  = M"001011--"		// LD r8,(FT+): loads through the hub pointer and step it, $2C-$2F (the r=2,3 B,C half is LD_BC_MEM; the r=0,1 F,T cells are the non-stepping $28/$29)
	def LD_BC_MEM  = M"0010101-"	// LD B,(FT+) / LD C,(FT+): the r=2,3 half of the stepping load row, $2A $2B
	def LD_T_MEM_INC = M"000101--"		// Load T from (R16), then step R16 up ($14-$17; $14 is the illegal FT pointer)
	def LD_F_MEM_INC = M"000111--"		// Load F from (R16), then step R16 up ($1C-$1F; $1C is the illegal FT pointer)
	def LD_R8_T    = M"01111---"		// Load register with T
	def LD_T_R8    = M"01011---"		// Load T with register
	def LS_FT_R8   = M"11110---"
	def OR_T_R8    = M"01100---"
	def RS_FT_R8   = M"11100---"
	def RSA_FT_R8  = M"11101---"
	def SUB_T_R8   = M"01010---"
	def XOR_T_R8   = M"01110---"

	def J_CC_I     = M"1001----"

}
