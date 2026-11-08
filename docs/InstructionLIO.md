# LIO (8 bit, load I/O, stepping)

## Opcode
| 76543 | 2 | 10 | Notes |
|-------|---|----|-------|
| 00110 | d | r  | d = 0 and r = 0 is `NOT F`; r = 0 loads do not step (see Notes) |

## Operation
Performs I/O access through the pair, then steps the pair. If I/O ports are
not supported, accesses memory instead.

```
if d = 0 then
    IO[Reg16[r]] <- T
else
    T <- IO[Reg16[r]]
Reg16[r] <- Reg16[r] + 1
PC <- PC + 1
```

The step costs no hardware: the pair's **old** value is used as the address
(and as the stored data's companion), and the incremented pair lands at the
write stage - so the access happens through the original pointer. The step
also follows the pair's register-stack top, and leaves the flags alone.

`LIO T,(FT)` (`$34`) is the exception: the loaded byte *is* `T`, a half of the
pointer being stepped, so FT stands still there. `LIO (FT),T` does not exist -
`$30` is `NOT F`.

The plain source spellings `LIO (BC),T` / `LIO T,(BC)` are assembled as the
stepped form plus a pointer rewind (`SUB R16,1`); see
[Synthesized instructions](SynthesizedInstructions.md).

## Assembler syntax examples
```
LIO T,(BC+)           ; stepped native, $35
LIO (DE+),T           ; stepped native, $32
LIO T,(BC)            ; synthesized: LIO T,(BC+) ; SUB BC,1
```

## See also
[LCO](InstructionLCO.md) ·
[Opcode matrix](OpcodeMatrix.md)
