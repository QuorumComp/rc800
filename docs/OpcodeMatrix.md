# Opcode Matrix

| xy rrr \ sss | 000         | 001         | 010         | 011         | 100         | 101         | 110         | 111         |
|--------------|-------------|-------------|-------------|-------------|-------------|-------------|-------------|-------------|
| 00_000       | NOP         | LD (BC),T   | LD (DE),T   | LD (HL),T   | LD T,(FT)   | LD T,(BC)   | LD T,(DE)   | LD T,(HL)   |
| 00_001       | LIO (BC),T  | LIO T,(BC)  | LCR (C),T   | LCR T,(C)   | LCO T,(FT)  | LCO T,(BC)  | LCO T,(DE)  | LCO T,(HL)  |
| 00_010       | EXG F       |             | EXG B       | EXG C       | EXG D       | EXG E       | EXG H       | EXG L       |
| 00_011       | NOT F       |             | EI          | DI          |             |             |             |             |
| 00_100       |             |             | LD (FT),B   | LD (FT),C   | LD (FT),D   | LD (FT),E   | LD (FT),H   | LD (FT),L   |
| 00_101       | LD F,(FT)   |             | LD B,(FT)   | LD C,(FT)   | LD D,(FT)   | LD E,(FT)   | LD H,(FT)   | LD L,(FT)   |
| 00_110       |             |             |             |             |             |             |             |             |
| 00_111       | JAL (FT)    | JAL (BC)    | JAL (DE)    | JAL (HL)    | J (FT)      | J (BC)      | J (DE)      | J (HL)      |
| 01_000       | ADD T,F     | ADD T,T     | ADD T,B     | ADD T,C     | ADD T,D     | ADD T,E     | ADD T,H     | ADD T,L     |
| 01_001       | CMP T,F     | EXT         | CMP T,B     | CMP T,C     | CMP T,D     | CMP T,E     | CMP T,H     | CMP T,L     |
| 01_010       | SUB T,F     | NEG T       | SUB T,B     | SUB T,C     | SUB T,D     | SUB T,E     | SUB T,H     | SUB T,L     |
| 01_011       | LD T,F      | RETI        | LD T,B      | LD T,C      | LD T,D      | LD T,E      | LD T,H      | LD T,L      |
| 01_100       | OR T,F      |             | OR T,B      | OR T,C      | OR T,D      | OR T,E      | OR T,H      | OR  T,L     |
| 01_101       | AND T,F     |             | AND T,B     | AND T,C     | AND T,D     | AND T,E     | AND T,H     | AND T,L     |
| 01_110       | XOR T,F     |             | XOR T,B     | XOR T,C     | XOR T,D     | XOR T,E     | XOR T,H     | XOR T,L     |
| 01_111       | LD F,T      |             | LD B,T      | LD C,T      | LD D,T      | LD E,T      | LD H,T      | LD L,T      |
| 10_000       | LD F,n8     | LD T,n8     | LD B,n8     | LD C,n8     | LD D,n8     | LD E,n8     | LD H,n8     | LD L,n8     |
| 10_001       | DJ F,n8     | DJ T,n8     | DJ B,n8     | DJ C,n8     | DJ D,n8     | DJ E,n8     | DJ H,n8     | DJ L,n8     |
| 10_010       | J/LE n8     | J/GT n8     | J/LT n8     | J/GE n8     | J/LEU n8    | J/GTU n8    | J/LTU n8    | J/GEU n8    |
| 10_011       | J/EQ n8     | J/NE n8     | J n8        | SYS n8      |             |             |             |             |
| 10_100       | ADD F,n8    | ADD T,n8    | ADD B,n8    | ADD C,n8    | ADD D,n8    | ADD E,n8    | ADD H,n8    | ADD L,n8    |
| 10_101       | CMP F,n8    | CMP T,n8    | CMP B,n8    | CMP C,n8    | CMP D,n8    | CMP E,n8    | CMP H,n8    | CMP L,n8    |
| 10_110       | OR T,n8     | AND T,n8    | XOR T,n8    |             |             |             |             |             |
| 10_111       | LS n8       | RS n8       |             | RSA n8      | ADD FT,n8   | ADD BC,n8   | ADD DE,n8   | ADD HL,n8   |
| 11_000       | PUSH FT     | PUSH BC     | PUSH DE     | PUSH HL     | POP FT      | POP BC      | POP DE      | POP HL      |
| 11_001       |             | EXG BC      | EXG DE      | EXG HL      |             | CMP FT,BC   | CMP FT,DE   | CMP FT,HL   |
| 11_010       |             | LD BC,FT    | LD DE,FT    | LD HL,FT    | TST FT      | TST BC      | TST DE      | TST HL      |
| 11_011       |             | LD FT,BC    | LD FT,DE    | LD FT,HL    |             |             |             |             |
| 11_100       |             |             | LS FT,B     | LS FT,C     | LS FT,D     | LS FT,E     | LS FT,H     | LS FT,L     |
| 11_101       |             |             | RS FT,B     | RS FT,C     | RS FT,D     | RS FT,E     | RS FT,H     | RS FT,L     |
| 11_110       | NEG FT      | SUB FT,BC   | SUB FT,DE   | SUB FT,HL   | ADD FT,FT   | ADD FT,BC   | ADD FT,DE   | ADD FT,HL   |
| 11_111       | PUSHA       | POPA        | RSA FT,B    | RSA FT,C    | RSA FT,D    | RSA FT,E    | RSA FT,H    | RSA FT,L    |


# Added
LD r8,(FT)
LD (FT),r8