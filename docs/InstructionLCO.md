# LCO (8 bit load from code memory, stepping)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 000011 | r  | r = 0 does not step (see Notes) |

## Operation
Loads a byte from code memory through the pair, then steps the pair.

```
T <- Code[Reg16[r]]
Reg16[r] <- Reg16[r] + 1
PC <- PC + 1
```

The step is the same zero-cost shape as the `$10-$1F` block: the access uses
the pair's old value and the incremented pair lands at the write stage, the
pair's register-stack top follows, and the flags are untouched. The loaded
byte arrives on the auxiliary write port while the main port steps.

`LCO T,(FT)` (`$0C`) is the exception: the loaded byte *is* `T`, a half of the
pointer being stepped, so FT stands still there.

The plain source spelling `LCO T,(BC)` is assembled as the stepped form plus
a pointer rewind (`SUB R16,1`); see
[Synthesized instructions](SynthesizedInstructions.md).

## Assembler syntax examples
```
LCO T,(BC+)           ; stepped native, $0D
LCO T,(DE+)           ; stepped native, $0E
LCO T,(BC)            ; synthesized: LCO T,(BC+) ; SUB BC,1
```

## See also
[LIO](InstructionLIO.md) ·
[Opcode matrix](OpcodeMatrix.md)
