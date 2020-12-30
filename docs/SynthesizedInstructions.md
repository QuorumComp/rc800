# Synthesized instructions

The assembler supports several synthesized instructions for improved quality of life.

| Assembler syntax | Instruction generated  |
|------------------|------------------------|
| JAL $1234        | LD HL,$1234 ; JAL (HL) |
| LD FT,$1234      | Several different sequences, from 3 to 4 bytes |
| PUSH BC-HL       | PUSHA; POP AF          |
| RETI/cc          | J/_cc,@+2 ; RETI       |
| ADD r16,n16      | ADD r16 ; ADD r8       |