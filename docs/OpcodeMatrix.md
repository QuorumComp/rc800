# Opcode Matrix
<div style="white-space:nowrap;">

| | 000         | 001         | 010         | 011         | 100         | 101         | 110         | 111         |
|--------------|-------------|-------------|-------------|-------------|-------------|-------------|-------------|-------------|
| 00000xxx       | NOP         | LD (BC),T   | LD (DE),T   | LD (HL),T   | LD T,(FT)   | LD T,(BC)   | LD T,(DE)   | LD T,(HL)   |
| 00001xxx       |             |             | LCR (C),T   | LCR T,(C)   | LCO T,(FT)  | LCO T,(BC)  | LCO T,(DE)  | LCO T,(HL)  |
| 00010xxx       | EXG T,F     |             | EXG T,B     | EXG T,C     | EXG T,D     | EXG T,E     | EXG T,H     | EXG T,L     |
| 00011xxx       | NOT F       |             | EI          | DI          | PICK FT     | PICK BC     | PICK DE     | PICK HL     |
| 00100xxx       |             |             | LD (FT),B   | LD (FT),C   | LD (FT),D   | LD (FT),E   | LD (FT),H   | LD (FT),L   |
| 00101xxx       | LD F,(FT)   |             | LD B,(FT)   | LD C,(FT)   | LD D,(FT)   | LD E,(FT)   | LD H,(FT)   | LD L,(FT)   |
| 00110xxx       |             | LIO (BC),T  | LIO (DE),T  | LIO (HL),T  | LIO T,(FT)  | LIO T,(BC)  | LIO T,(DE)  | LIO T,(HL)  |
| 00111xxx       | JAL (FT)    | JAL (BC)    | JAL (DE)    | JAL (HL)    | J (FT)      | J (BC)      | J (DE)      | J (HL)      |
| 01000xxx       | ADD T,F     | ADD T,T     | ADD T,B     | ADD T,C     | ADD T,D     | ADD T,E     | ADD T,H     | ADD T,L     |
| 01001xxx       | CMP T,F     | EXT         | CMP T,B     | CMP T,C     | CMP T,D     | CMP T,E     | CMP T,H     | CMP T,L     |
| 01010xxx       | SUB T,F     | NEG T       | SUB T,B     | SUB T,C     | SUB T,D     | SUB T,E     | SUB T,H     | SUB T,L     |
| 01011xxx       | LD T,F      | RETI        | LD T,B      | LD T,C      | LD T,D      | LD T,E      | LD T,H      | LD T,L      |
| 01100xxx       | OR T,F      |             | OR T,B      | OR T,C      | OR T,D      | OR T,E      | OR T,H      | OR  T,L     |
| 01101xxx       | AND T,F     |             | AND T,B     | AND T,C     | AND T,D     | AND T,E     | AND T,H     | AND T,L     |
| 01110xxx       | XOR T,F     |             | XOR T,B     | XOR T,C     | XOR T,D     | XOR T,E     | XOR T,H     | XOR T,L     |
| 01111xxx       | LD F,T      |             | LD B,T      | LD C,T      | LD D,T      | LD E,T      | LD H,T      | LD L,T      |
| 10000xxx       | LD F,i8     | LD T,i8     | LD B,i8     | LD C,i8     | LD D,i8     | LD E,i8     | LD H,i8     | LD L,i8     |
| 10001xxx       | DJ F,s8     | DJ T,s8     | DJ B,s8     | DJ C,s8     | DJ D,s8     | DJ E,s8     | DJ H,s8     | DJ L,s8     |
| 10010xxx       | J/LE s8     | J/GT s8     | J/LT s8     | J/GE s8     | J/LEU s8    | J/GTU s8    | J/LTU s8    | J/GEU s8    |
| 10011xxx       | J/EQ s8     | J/NE s8     | J s8        | SYS i8      |             |             |             |             |
| 10100xxx       | ADD F,i8    | ADD T,i8    | ADD B,i8    | ADD C,i8    | ADD D,i8    | ADD E,i8    | ADD H,i8    | ADD L,i8    |
| 10101xxx       | CMP F,i8    | CMP T,i8    | CMP B,i8    | CMP C,i8    | CMP D,i8    | CMP E,i8    | CMP H,i8    | CMP L,i8    |
| 10110xxx       | OR T,i8     | AND T,i8    | XOR T,i8    |             |             |             |             |             |
| 10111xxx       | LS FT,i8    | RS FT,i8    |             | RSA FT,i8   | ADD FT,s8   | ADD BC,s8   | ADD DE,s8   | ADD HL,s8   |
| 11000xxx       | PUSH FT     | PUSH BC     | PUSH DE     | PUSH HL     | POP FT      | POP BC      | POP DE      | POP HL      |
| 11001xxx       |             | EXG FT,BC   | EXG FT,DE   | EXG FT,HL   | SWAPA       | CMP FT,BC   | CMP FT,DE   | CMP FT,HL   |
| 11010xxx       |             | LD BC,FT    | LD DE,FT    | LD HL,FT    | TST FT      | TST BC      | TST DE      | TST HL      |
| 11011xxx       |             | LD FT,BC    | LD FT,DE    | LD FT,HL    | SWAP FT     | SWAP BC     | SWAP DE     | SWAP HL     |
| 11100xxx       |             |             | LS FT,B     | LS FT,C     | LS FT,D     | LS FT,E     | LS FT,H     | LS FT,L     |
| 11101xxx       |             |             | RS FT,B     | RS FT,C     | RS FT,D     | RS FT,E     | RS FT,H     | RS FT,L     |
| 11110xxx       | NEG FT      | SUB FT,BC   | SUB FT,DE   | SUB FT,HL   | ADD FT,FT   | ADD FT,BC   | ADD FT,DE   | ADD FT,HL   |
| 11111xxx       | PUSHA       | POPA        | RSA FT,B    | RSA FT,C    | RSA FT,D    | RSA FT,E    | RSA FT,H    | RSA FT,L    |
</div>

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
