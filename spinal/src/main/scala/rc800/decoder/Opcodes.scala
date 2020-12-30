package rc800.decoder

import spinal.core._

object Opcodes {
	def illegals = List(
		M"00010001",
		M"00011001",
		M"000111--",
		M"0010000-",
		M"00101001",
		M"00110---",
		M"01100001",
		M"01101001",
		M"01110001",
		M"01111001",
		M"100111--",
		M"10110011",
		M"101101--",
		M"10111010",
		M"11001000",
		M"11001100",
		M"11010000",
		M"11011000",
		M"110111--",
		M"1110000-",
		M"1110100-",
	)


	def AND_T_I    = M"10110001"
	def DI         = M"00011011"
	def EI         = M"00011010"
	def EXT_T      = M"01001001"
	def LD_CR_T    = M"00001010"
	def LD_IO_T    = M"00001000"		// Load I/O with T
	def LD_T_CR    = M"00001011"
	def LD_T_IO    = M"00001001"		// Load T with I/O
	def LS_FT_I    = M"10111000"
	def NEG_T      = M"01010001"
	def NEG_FT     = M"11110000"
	def NOP        = M"00000000"
	def NOT_F      = M"00011000"
	def OR_T_I     = M"10110000"
	def POPA       = M"11111001"
	def PUSHA      = M"11111000"
	def RETI       = M"01011001"
	def RS_FT_I    = M"10111001"
	def RSA_FT_I   = M"10111011"
	def SYS_I      = M"10011011"
	def XOR_T_I    = M"10110010"

	def ADD_FT_R16 = M"111101--"
	def ADD_R16_I  = M"101111--"
	def CMP_FT_R16 = M"110011--"
	def EXG_FT_R16 = M"110010--"
	def JAL_R16    = M"001110--"
	def J_R16      = M"001111--"
	def LD_FT_R16  = M"110110--"		// Load FT with register pair
	def LD_MEM_T   = M"000000--"		// Store T in memory
	def LD_R16_FT  = M"110100--"		// Load register pair with FT
	def LD_T_CODE  = M"000011--"
	def LD_T_MEM   = M"000001--"		// Load T from memory
	def POP        = M"110001--"
	def PUSH       = M"110000--"
	def SUB_FT_R16 = M"111100--"
	def TST_R16    = M"110101--"

	def ADD_T_R8   = M"01000---"
	def ADD_R8_I   = M"10100---"
	def AND_T_R8   = M"01101---"
	def CMP_R8_I   = M"10101---"
	def CMP_T_R8   = M"01001---"
	def DJ_R8_I    = M"10001---"
	def EXG_T_R8   = M"00010---"
	def LD_MEM_R8  = M"00100---"
	def LD_R8_I    = M"10000---"		// Load 8 bit immediate
	def LD_R8_MEM  = M"00101---"
	def LD_R8_T    = M"01111---"		// Load register with T
	def LD_T_R8    = M"01011---"		// Load T with register
	def LS_FT_R8   = M"11100---"
	def OR_T_R8    = M"01100---"
	def RS_FT_R8   = M"11101---"
	def RSA_FT_R8  = M"11111---"
	def SUB_T_R8   = M"01010---"
	def XOR_T_R8   = M"01110---"

	def J_CC_I     = M"1001----"

}
