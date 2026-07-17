# Synthesized instructions

The assembler supports several synthesized instructions for improved quality of life. They expand into safe sequences of native instructions with no unintended side effects.

**Cycle counting:** each synthesized instruction expands to the number of native instructions listed in its expansion. Every native instruction costs 4 cycles. When counting cycles or instructions per loop iteration, count the *expanded* form, not the source form — for example, `LD T,(BC+)` is one source instruction but two native instructions (`LD T,(BC)` + `ADD BC,1` = 8 cycles).

## Immediate Loading

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LD FT,i16` | Optimized: `LD T,xx` / `EXT` (if high byte is 0), `LD T,xx` / `LD F,T` (if both bytes equal), or `LD F,high` / `LD T,low` (general case) |
| `LD BC,i16` | `LD B,high` / `LD C,low` |
| `LD DE,i16` | `LD D,high` / `LD E,low` |
| `LD HL,i16` | `LD H,high` / `LD L,low` |

## Arithmetic with Immediates (SUB)

Native `SUB` supports only register-to-register. These forms are synthesized by expanding to `ADD` with a negated immediate. They do **not** set F flags — only `CMP` and `TST` set flags.

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `SUB R8,i8` | `ADD R8,-i8` |
| `SUB FT,s8` | `ADD FT,-s8` |
| `SUB BC,s8` | `ADD BC,-s8` |
| `SUB DE,s8` | `ADD DE,-s8` |
| `SUB HL,s8` | `ADD HL,-s8` |
| `SUB FT,i16` | `ADD FT,-i16` (further expanded) |
| `SUB BC,i16` | `ADD BC,-i16` (further expanded) |
| `SUB DE,i16` | `ADD DE,-i16` (further expanded) |
| `SUB HL,i16` | `ADD HL,-i16` (further expanded) |

## 16-bit ADD with Immediate

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `ADD FT,i16` | `ADD FT,s8` if it fits; otherwise split into low-byte and high-byte adjustments |
| `ADD BC,i16` | `ADD BC,s8` if it fits; otherwise split into low-byte and high-byte adjustments |
| `ADD DE,i16` | `ADD DE,s8` if it fits; otherwise split into low-byte and high-byte adjustments |
| `ADD HL,i16` | `ADD HL,s8` if it fits; otherwise split into low-byte and high-byte adjustments |

## NOT

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `NOT T` | `XOR T,$FF` |
| `NOT FT` | `XOR T,$FF` / `NOT F` |

## Jump and Link with Immediate

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `JAL i16` | `LD HL,i16` / `JAL (HL)` |

## Post-increment and Pre-decrement Memory Access

The native instruction set does not include auto-increment/decrement addressing. These forms are synthesized for any 16-bit register pair (`FT`, `BC`, `DE`, `HL`).

**Target constraint:** the post-increment and pre-decrement load forms listed below target `T` only. There is no `LD C,(DE+)`, `LD B,(BC+)`, etc. — to load an 8-bit register other than `T` through a post-incrementing non-FT pointer, either load into `T` and transfer via `LD R8,T`, or use `FT` as the pointer (the `LD R8,(FT+)` / `LD R8,(-FT)` forms are synthesized separately; see [8-bit Memory Access through FT with Post-increment/Pre-decrement](#8-bit-memory-access-through-ft-with-post-incrementpre-decrement)).

### Expansions

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LD T,(R16+)` | `LD T,(R16)` / `ADD R16,1` |
| `LD (R16+),T` | `LD (R16),T` / `ADD R16,1` |
| `LD T,(-R16)` | `ADD R16,-1` / `LD T,(R16)` |
| `LD (-R16),T` | `ADD R16,-1` / `LD (R16),T` |

## 16-bit Memory Loads/Stores through R16

Loading or storing a full 16-bit register pair through a non-FT pointer requires synthesized sequences.

### Expansions

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LD FT,(R16)` | `LD T,(R16)` / `ADD R16,1` / `EXG F,T` / `LD T,(R16)` / `ADD R16,-1` / `EXG F,T` |
| `LD (R16),FT` | `LD (R16),T` / `ADD R16,1` / `EXG F,T` / `LD (R16),T` / `ADD R16,-1` / `EXG F,T` |
| `LD FT,(R16+)` | `LD T,(R16)` / `ADD R16,1` / `EXG F,T` / `LD T,(R16)` / `EXG F,T` |
| `LD (R16+),FT` | `LD (R16),T` / `ADD R16,1` / `EXG F,T` / `LD (R16),T` / `EXG F,T` |
| `LD FT,(-R16)` | `LD T,(R16)` / `ADD R16,-1` / `EXG F,T` / `LD T,(R16)` |
| `LD (-R16),FT` | `EXG F,T` / `LD (R16),T` / `ADD R16,-1` / `EXG F,T` / `LD (R16),T` |

## 16-bit Memory Loads/Stores through FT

Loading or storing a full 16-bit register pair through `FT` also uses synthesized sequences. These temporarily swap `F` and `T` via `EXG F,T` to avoid corrupting the `FT` pointer.

### Expansions

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LD R16,(FT)` | `LD low,(FT)` / `ADD FT,1` / `LD high,(FT)` / `ADD FT,-1` |
| `LD (FT),R16` | `LD (FT),low` / `ADD FT,1` / `LD (FT),high` / `ADD FT,-1` |
| `LD R16,(FT+)` | `LD low,(FT)` / `ADD FT,1` / `LD high,(FT)` |
| `LD (FT+),R16` | `LD (FT),low` / `ADD FT,1` / `LD (FT),high` |
| `LD R16,(-FT)` | `LD high,(FT)` / `ADD FT,-1` / `LD low,(FT)` |
| `LD (-FT),R16` | `ADD FT,-1` / `LD (FT),high` / `LD (FT),low` |

## 8-bit Memory Access through FT with Post-increment/Pre-decrement

The native `LD R8,(FT)` and `LD (FT),R8` instructions transfer any 8-bit register (B, C, D, E, H, L) through the FT pointer without corrupting it — unlike `LD T,(FT)` which overwrites T. Their post-increment and pre-decrement forms are synthesized by appending or prepending `ADD FT,±1`. `F` is excluded because `LD F,(FT)` destroys the FT pointer, and `T` is covered by the [Post-increment and Pre-decrement Memory Access](#post-increment-and-pre-decrement-memory-access) section above.

### Expansions

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LD R8,(FT+)` | `LD R8,(FT)` / `ADD FT,1` |
| `LD (FT+),R8` | `LD (FT),R8` / `ADD FT,1` |
| `LD R8,(-FT)` | `ADD FT,-1` / `LD R8,(FT)` |
| `LD (-FT),R8` | `ADD FT,-1` / `LD (FT),R8` |

## LIO with Post-increment/Pre-decrement

Supported for any 16-bit register pair (`FT`, `BC`, `DE`, `HL`).

### Expansions

`LIO` follows the same expansion pattern as the corresponding `LD` form: replace `LD` with `LIO` in each line. The `IO` access-type signal is asserted through each native step.

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LIO T,(R16+)` | `LIO T,(R16)` / `ADD R16,1` |
| `LIO (R16+),T` | `LIO (R16),T` / `ADD R16,1` |
| `LIO T,(-R16)` | `ADD R16,-1` / `LIO T,(R16)` |
| `LIO (-R16),T` | `ADD R16,-1` / `LIO (R16),T` |

## LCO with Post-increment/Pre-decrement

Supported for any 16-bit register pair (`FT`, `BC`, `DE`, `HL`). `LCO` is load-only (no store form).

### Expansions

Load-only subset of the `LD` pattern. The `CODE` access-type signal is asserted through each native step.

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `LCO T,(R16+)` | `LCO T,(R16)` / `ADD R16,1` |
| `LCO T,(-R16)` | `ADD R16,-1` / `LCO T,(R16)` |

## Multi-register Stack Operations

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `PUSH BC-HL` | `PUSHA` / `POP FT` (push all except FT) |
| `POP BC-HL` | `POPA` / `PUSH FT` (pop all except FT) |
| `PUSH R16/R16` | Individual `PUSH` per listed register |
| `POP R16/R16` | Individual `POP` per listed register |
| `SWAP R16/R16` | Individual `SWAP` per listed register |

Register range syntax uses `-` for ranges and `/` for individual registers. Any combination is accepted, e.g., `PUSH FT/HL`, `PUSH BC-HL`, `PUSH BC-DE`.

## EXG Register-to-Register (not involving T/FT)

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `EXG R8,R8` | `EXG T,src` / `EXG T,dest` / `EXG T,src` (3-exchange via T) |
| `EXG R16,R16` | `EXG FT,src` / `EXG FT,dest` / `EXG FT,src` (3-exchange via FT) |

## PICK with Immediate

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `PICK R16,i8` | `LD low_reg,i8` / `PICK R16`, where `low_reg` is the low byte of `R16` |

The immediate form loads the low byte of the register pair with the index value, then executes `PICK`. This is a shorthand that avoids manually setting the register to the index value first.

## Conditional Execution — INST/CC

Any instruction can be made conditional by appending `/CC`. The assembler emits a native `J/!CC` (inverted condition) that skips the instruction if the condition is not met. For example, `LD/EQ T,1` becomes `J/NE,@+2` / `LD T,1`.

| Assembler syntax | Instruction generated | Notes |
|------------------|------------------------|-------|
| `INST/CC operands` | `J/!CC,@+n` / `INST operands` | Any native instruction + any CC |
| `J/CC (R16)` | `J/!CC,@+2` / `J (R16)` | 10 CCs × 4 R16 = 40 permutations |
| `RETI/CC` | `J/!CC,@+2` / `RETI` | Conditional return from interrupt |

The `!CC` is the inverse condition (for example, `EQ` → `NE`, `LT` → `GE`).

## Notes

- **Expansion:** Each synthesized instruction expands to a safe sequence of native instructions with no unintended side effects. The exact expansion may vary based on assembler optimizations.
- **SUB synthesized forms:** Do **not** set F flags — only `CMP` and `TST` set flags.
- **Post-increment/pre-decrement:** The register is modified as a side effect of the synthesized sequence.

# Continue reading

[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
