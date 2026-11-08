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

Loading or storing a full 16-bit register pair through a non-FT pointer (`BC`/`DE`/`HL`) borrows that pointer through `EXG FT,R16`, runs two native stepped-byte accesses, then returns the stepped pointer. `R16.lo` / `R16.hi` are the pointer pair's own byte halves (little-endian, low byte first). These contain no ALU op except the pre-decrement `ADD R16,-2`, so they leave the condition flags untouched.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD FT,(R16)` | `EXG FT,R16` / `LD R16.lo,(FT+)` / `LD R16.hi,(FT)` / `ADD FT,-1` / `EXG FT,R16` | 5 |
| `LD (R16),FT` | `EXG FT,R16` / `LD (FT+),R16.lo` / `LD (FT),R16.hi` / `ADD FT,-1` / `EXG FT,R16` | 5 |
| `LD FT,(R16+)` | `EXG FT,R16` / `LD R16.lo,(FT+)` / `LD R16.hi,(FT+)` / `EXG FT,R16` | 4 |
| `LD (R16+),FT` | `EXG FT,R16` / `LD (FT+),R16.lo` / `LD (FT+),R16.hi` / `EXG FT,R16` | 4 |
| `LD FT,(-R16)` | `ADD R16,-2` / `EXG FT,R16` / `LD R16.lo,(FT+)` / `LD R16.hi,(FT)` / `ADD FT,-1` / `EXG FT,R16` | 6 |
| `LD (-R16),FT` | `ADD R16,-2` / `EXG FT,R16` / `LD (FT+),R16.lo` / `LD (FT),R16.hi` / `ADD FT,-1` / `EXG FT,R16` | 6 |

## 16-bit Memory Loads/Stores through FT

When `FT` is the pointer and `BC`/`DE`/`HL` is the data, each byte step is a native stepped-byte opcode (`LD R8,(FT+)` / `LD (FT+),R8`), low byte first. Only the `ADD FT,±N` pointer adjustment touches the ALU, so the flags are left untouched.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD R16,(FT)` | `LD R16.lo,(FT+)` / `LD R16.hi,(FT)` / `ADD FT,-1` | 3 |
| `LD (FT),R16` | `LD (FT+),R16.lo` / `LD (FT),R16.hi` / `ADD FT,-1` | 3 |
| `LD R16,(FT+)` | `LD R16.lo,(FT+)` / `LD R16.hi,(FT+)` | 2 |
| `LD (FT+),R16` | `LD (FT+),R16.lo` / `LD (FT+),R16.hi` | 2 |
| `LD R16,(-FT)` | `ADD FT,-2` / `LD R16.lo,(FT+)` / `LD R16.hi,(FT)` / `ADD FT,-1` | 4 |
| `LD (-FT),R16` | `ADD FT,-2` / `LD (FT+),R16.lo` / `LD (FT),R16.hi` / `ADD FT,-1` | 4 |

## 8-bit Memory Access through FT with Post-increment/Pre-decrement

The native `LD R8,(FT)` and `LD (FT),R8` instructions transfer any 8-bit register (B, C, D, E, H, L) through the FT pointer without corrupting it — unlike `LD T,(FT)` which overwrites T. Their **post-increment forms are native** — `LD R8,(FT+)` (`$1A-$1F`) and `LD (FT+),R8` (`$12-$17`) advance FT by 1 in hardware, in a single instruction. Their **pre-decrement forms are synthesized** by prepending `ADD FT,-1` (there is no native pre-decrement byte opcode). `F` is rejected from all four stepped forms: writing `F` clobbers the very FT pointer being stepped, so a stepped `F` access is a bug rather than a synthesis (the plain `LD F,(FT)` / `LD (FT),F` remain valid). `T` is covered by the [Post-increment and Pre-decrement Memory Access](#post-increment-and-pre-decrement-memory-access) section above.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD R8,(FT+)` | native stepped-byte opcode (`$1A-$1F`) | 1 |
| `LD (FT+),R8` | native stepped-byte opcode (`$12-$17`) | 1 |
| `LD R8,(-FT)` | `ADD FT,-1` / `LD R8,(FT)` | 2 |
| `LD (-FT),R8` | `ADD FT,-1` / `LD (FT),R8` | 2 |

## LIO with Post-increment/Pre-decrement

Supported for any 16-bit register pair (`FT`, `BC`, `DE`, `HL`). The `IO` access-type signal is asserted through each native step.

**Byte forms** follow the `LD` byte pattern (replace `LD` with `LIO`):

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LIO T,(R16+)` | `LIO T,(R16)` / `ADD R16,1` | 2 |
| `LIO (R16+),T` | `LIO (R16),T` / `ADD R16,1` | 2 |
| `LIO T,(-R16)` | `ADD R16,-1` / `LIO T,(R16)` | 2 |
| `LIO (-R16),T` | `ADD R16,-1` / `LIO (R16),T` | 2 |

**Word forms differ from `LD`.** There is no native stepped I/O opcode, so a full-word `LIO` cannot borrow the pointer through `EXG FT,R16` and run stepped bytes; it walks the pair pointer with two single-byte I/O accesses, an `EXG F,T` between them (which also preserves `FT` across a store), and explicit `ADD`s. Word forms exist only through a **general pair** (`BC`/`DE`/`HL`) — a word form through `FT` is an operand error. Every word form is more expensive than the matching `LD`:

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LIO FT,(R16)` | `LIO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LIO T,(R16)` / `ADD R16,-1` / `EXG F,T` | 6 |
| `LIO (R16),FT` | `LIO (R16),T` / `ADD R16,1` / `EXG F,T` / `LIO (R16),T` / `ADD R16,-1` / `EXG F,T` | 6 |
| `LIO FT,(R16+)` | `LIO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LIO T,(R16)` / `ADD R16,1` / `EXG F,T` | 6 |
| `LIO (R16+),FT` | `LIO (R16),T` / `ADD R16,1` / `EXG F,T` / `LIO (R16),T` / `ADD R16,1` / `EXG F,T` | 6 |
| `LIO FT,(-R16)` | `ADD R16,-2` / `LIO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LIO T,(R16)` / `ADD R16,-1` / `EXG F,T` | 7 |
| `LIO (-R16),FT` | `ADD R16,-2` / `LIO (R16),T` / `ADD R16,1` / `EXG F,T` / `LIO (R16),T` / `ADD R16,-1` / `EXG F,T` | 7 |

## LCO with Post-increment/Pre-decrement

Supported for byte accesses on any 16-bit register pair (`FT`, `BC`, `DE`, `HL`); word accesses only through a general pair, as with `LIO`. `LCO` is load-only (no store form), so `LCO (R16),FT` and its stepped forms are operand errors. The `CODE` access-type signal is asserted through each native step. The **byte** forms follow the `LD` byte pattern; the **word load** forms follow the `LIO` word pattern above (same instruction counts, `LCO` in place of `LIO`):

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LCO T,(R16+)` | `LCO T,(R16)` / `ADD R16,1` | 2 |
| `LCO T,(-R16)` | `ADD R16,-1` / `LCO T,(R16)` | 2 |
| `LCO FT,(R16)` | `LCO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LCO T,(R16)` / `ADD R16,-1` / `EXG F,T` | 6 |
| `LCO FT,(R16+)` | `LCO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LCO T,(R16)` / `ADD R16,1` / `EXG F,T` | 6 |
| `LCO FT,(-R16)` | `ADD R16,-2` / `LCO T,(R16)` / `ADD R16,1` / `EXG F,T` / `LCO T,(R16)` / `ADD R16,-1` / `EXG F,T` | 7 |

## Expansion cost and ranking

Every native instruction costs 4 cycles. The tables below rank the indirect/stepped access forms by how many native instructions each expands to — fastest first. The pointer you choose and whether you step matter more than you might expect.

### 16-bit access, ranked (fewest instructions first)

| Rank | Form | Instr | Cycles | Notes |
|---|------|-------|--------|-------|
| 1 | `LD R16,(FT+)` / `LD (FT+),R16` | 2 | 8 | Two native stepped bytes — the cheapest 16-bit access |
| 2 | `LD R16,(FT)` / `LD (FT),R16` | 3 | 12 | FT-pointer plain (adds `ADD FT,-1` to restore the pointer) |
| 3 | `LD FT,(R16+)` / `LD (R16+),FT` | 4 | 16 | Pair-pointer post-increment |
| 3 | `LD R16,(-FT)` / `LD (-FT),R16` | 4 | 16 | FT-pointer pre-decrement |
| 5 | `LD FT,(R16)` / `LD (R16),FT` | 5 | 20 | Pair-pointer plain |
| 6 | `LD FT,(-R16)` / `LD (-R16),FT` | 6 | 24 | Pair-pointer pre-decrement |
| 7 | `LIO` / `LCO` word, plain or post-inc `(R16)`/`(R16+)` | 6 | 24 | No native stepped I/O opcode; word through a pair only |
| 8 | `LIO` / `LCO` word, pre-dec `(-R16)` | 7 | 28 | |

### Byte access, for reference

| Form | Instr | Cycles |
|------|-------|--------|
| `LD T,(R16)` / `LD (R16),T` / `LD R8,(FT)` / `LD (FT),R8` | 1 | 4 |
| `LD R8,(FT+)` / `LD (FT+),R8` | 1 | 4 | native stepped-byte opcode |
| `LD T,(R16+)` / `LD T,(-R16)` / `LD R8,(-FT)` / … | 2 | 8 |

### What surprises people

- **Post-increment is cheaper than plain.** `LD FT,(R16+)` (4) beats `LD FT,(R16)` (5); `LD R16,(FT+)` (2) beats `LD R16,(FT)` (3). The plain form must walk the borrowed pointer back with `ADD FT,-1`; the post-increment form just runs two stepped bytes and lets the pointer land where it lands. When the pointer's final value is dead — or you can compensate a neighbouring offset — prefer `(R16+)`: it is shorter *and* faster.
- **Using `FT` as the pointer is much cheaper than `BC`/`DE`/`HL`.** A word through FT costs 2–3 instructions; the same word through a general pair costs 4–6, because FT has native stepped-byte opcodes while a general pair must be borrowed through `EXG FT,R16`. When FT is free, route the access through it.
- **Pre-decrement is the most expensive** stepping mode — it adds `ADD ±2` plus the walk-back.
- **Byte stepped accesses through FT are a single native instruction**, but the same through `BC`/`DE`/`HL` costs two (no native stepped byte for those pointers).
- **`LIO`/`LCO` word accesses cost more than `LD`** (6–7 versus 2–6) because there is no native stepped I/O opcode to borrow the pointer.

## Multi-register Stack Operations

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `PUSH BC/DE/HL` | `PUSHA` / `POP FT` (push all except FT) |
| `POP BC/DE/HL` | `POPA` / `PUSH FT` (pop all except FT) |
| `PUSH R16/R16` | Individual `PUSH` per listed register |
| `POP R16/R16` | Individual `POP` per listed register |
| `SWAP R16/R16` | Individual `SWAP` per listed register |

Register lists use `/` to separate registers, e.g., `PUSH FT/HL`, `PUSH BC/DE/HL`.

## EXG Register-to-Register (not involving T/FT)

| Assembler syntax | Instruction generated |
|------------------|------------------------|
| `EXG R16,R16` | `EXG FT,src` / `EXG FT,dest` / `EXG FT,src` (3-exchange via FT) |

There is no byte-to-byte exchange: the assembler does not synthesize one, because its expansion needed `EXG T,R8` (`$12-$17`), which is an illegal opcode (see `ISA-V2-WISHLIST.md` A1). `EXG B,C` and friends are operand errors. A byte swap is three `LD`s through `T`, or a pair exchange when the bytes are low halves of pairs.

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
