# Synthesized instructions

The assembler supports several synthesized instructions for improved quality of life. They expand into safe sequences of native instructions with no unintended side effects.

**Cycle counting:** each synthesized instruction expands to the number of native instructions listed in its expansion. Every native instruction costs 4 cycles. When counting cycles or instructions per loop iteration, count the *expanded* form, not the source form — for example, `LD C,(FT+)` is one source instruction but two native instructions (`LD C,(FT)` + `ADD FT,1` = 8 cycles).

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

## Post-increment Memory Access

**Byte post-increment through a general pointer is native.** `LD T,(R16+)`, `LD (R16+),T`, `LD F,(R16+)` and `LD (R16+),F` over `BC`/`DE`/`HL` are the twelve stepped encodings of `$10-$1F` (see the [opcode matrix](OpcodeMatrix.md)); the byte operand is a hub byte — `F` or `T` — and the stepped pointer is the pair named by the standard r16 field. A *hub byte* through `FT` as the stepped pointer is illegal: it would land inside the pointer being stepped. Byte registers through `FT` do step natively, on their own rows (`$22–$27`, `$2A–$2F`).

**Pre-decrement is retired** (2026-11-12): stepped accesses only step up, and `(-R16)` / `(-FT)` byte and word forms are operand errors.

**Plain byte access through any pointer is synthesized.** The plain pair byte forms retired from the ISA 2026-11-12 (`$01–$07` are all illegal), and the plain FT byte forms retired the same week: `LD (FT),R8` / `LD R8,(FT)` are now the stepped access plus a `SUB FT,1` walk-back, and the cells themselves step (`$22–$27`, `$2A–$2F`). On 2026-11-13 the I/O and code byte rows joined: the plain `LIO` / `LCO` spellings synthesize as the stepped access plus `SUB R16,1`. The only plain byte accesses left in one instruction are the two hub-byte loads, `LD F,(FT)` (`$28`) and `LD T,(FT)` (`$29`), which cannot step because the loaded byte is a half of the pointer.

Everything else stepping through `FT` is synthesized: every 16-bit stepped access.

**Target constraint:** the native post-increment byte forms through a general pair target the hub bytes `T` and `F` only. There is no `LD C,(DE+)`, `LD B,(BC+)`, etc. — to load an 8-bit register other than `T`/`F` through a post-incrementing pair pointer, either load into `T` and transfer via `LD R8,T`, or use `FT` as the pointer, where the byte-register stepped forms `LD R8,(FT+)` / `LD (FT+),R8` are native (see [8-bit Memory Access through FT with Post-increment](#8-bit-memory-access-through-ft-with-post-increment)).

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD T,(R16+)` / `LD (R16+),T` / `LD F,(R16+)` / `LD (R16+),F` | native stepped opcode (`$11-$13`, `$15-$17`, `$19-$1B`, `$1D-$1F`) | 1 |
| `LD F,(FT)` / `LD T,(FT)` | native `$28` / `$29` | 1 |
| `LD T,(R16)` | `LD T,(R16+)` / `ADD R16,-1` | 2 |
| `LD (R16),T` | `LD (R16+),T` / `ADD R16,-1` | 2 |

The post-increment forms access the byte under the pointer and add 1 afterwards; the synthesized plain forms walk that step back, leaving the pointer where a plain access would have left it. Neither direction sets the flags as a side effect — only `CMP` and `TST` write `F` implicitly, and the pointer bump is part of the access rather than an `ADD`. Moving `F` is the exception by definition: `LD F,(R16+)` *replaces* the condition codes with the byte loaded, and `LD (R16+),F` reads them, which is exactly the spill/refill and carry-lane use the `F` forms exist for.

Because the plain pair byte forms are synthesized, **post-increment is cheaper than plain** for the same pointer: `LD T,(R16+)` is 1 byte and 1 instruction against the plain form's 3 bytes and 2 instructions. When the pointer's end value is dead or compensable, prefer `(R16+)`.

## 16-bit Memory Loads/Stores through R16

A full 16-bit access through a general pointer is two native stepped byte accesses, low byte first. The two steps always leave the pointer two above where it started, so the plain form walks it back (`ADD R16,-2`). The walk-back `ADD` does not touch the condition codes (only `CMP` and `TST` write `F`), but the access itself does: the second step is `LD F,(R16+)`, and `F` is the flags. A 16-bit load or store through a pointer therefore **destroys the condition codes** — re-test after it, or keep the test on the far side of the access.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD FT,(R16+)` | `LD T,(R16+)` / `LD F,(R16+)` | 2 |
| `LD (R16+),FT` | `LD (R16+),T` / `LD (R16+),F` | 2 |
| `LD FT,(R16)` | `LD T,(R16+)` / `LD F,(R16+)` / `ADD R16,-2` | 3 |
| `LD (R16),FT` | `LD (R16+),T` / `LD (R16+),F` / `ADD R16,-2` | 3 |

## 16-bit Memory Loads/Stores through FT

When `FT` is the pointer and `BC`/`DE`/`HL` is the data, the pair is borrowed: `EXG FT,R16` lends the pointer to the pair, the two native stepped bytes run through it (`LD T,(R16+)` / `LD F,(R16+)` — the hub halves carry the loaded word), and `EXG FT,R16` returns the stepped pointer and the loaded word. A store keeps its data in `R16` across the swap. As through a general pair, the pointer ends two above the access, so the plain form walks it back with `ADD FT,-2`. Only those `ADD`s touch the ALU, and they leave the condition codes alone — but the access itself does not: the word lands in `FT`, whose high half is `F`, so a load through a borrowed pointer replaces the flags along with the rest of `FT`, and the exchange that returns it is what puts them there.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD R16,(FT+)` | `EXG FT,R16` / `LD T,(R16+)` / `LD F,(R16+)` / `EXG FT,R16` | 4 |
| `LD (FT+),R16` | `EXG FT,R16` / `LD (R16+),T` / `LD (R16+),F` / `EXG FT,R16` | 4 |
| `LD R16,(FT)` | `EXG FT,R16` / `LD T,(R16+)` / `LD F,(R16+)` / `EXG FT,R16` / `ADD FT,-2` | 5 |
| `LD (FT),R16` | `EXG FT,R16` / `LD (R16+),T` / `LD (R16+),F` / `EXG FT,R16` / `ADD FT,-2` | 5 |

## 8-bit Memory Access through FT with Post-increment

The native `LD R8,(FT+)` and `LD (FT+),R8` instructions (`$2A–$2F`, `$22–$27`) transfer any 8-bit register (B, C, D, E, H, L) through the `FT` pointer and step it, taking over the plain-access cells on 2026-11-12. The plain spelling is now the synthesized one: post-increment by the native opcode, plain by appending `SUB FT,1` to the stepped access.

The hub bytes are the exception in both directions. A load of `F` or `T` through `FT` writes the half of the very pointer a step would install, so it cannot step and stays plain — the native `LD F,(FT)` (`$28`) and `LD T,(FT)` (`$29`). A store of `F` or `T` through `FT` has no form at all (`LD (FT),F` and `LD (FT),T` are illegal cells; a stepped one would land the byte inside the pointer). `LD (FT+),F` and `LD (FT+),T` are therefore rejected — reach the byte through a general pair instead, where the hub-byte step is a single native instruction.

### Expansions

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LD R8,(FT+)` / `LD (FT+),R8` | native stepped opcode (`$2A-$2F` / `$22-$27`) | 1 |
| `LD R8,(FT)` / `LD (FT),R8` | `LD R8,(FT+)` / `SUB FT,1` | 2 |

## LIO with Post-increment

Supported for any 16-bit register pair (`FT`, `BC`, `DE`, `HL`). The `IO` access-type signal is asserted through each native step.

**Byte forms are native since 2026-11-13** (`$31–$33` store, `$35–$37` load): one instruction does the access and the step, like the `LD` block. The plain spellings synthesize as the stepped form plus a walk-back. (`LIO T,(FT)` at `$34` is the exception cell that cannot step; `LIO (FT),T` does not exist — `$30` is `NOT F`.)

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LIO T,(R16+)` / `LIO (R16+),T` | native stepped opcode (`$35–$37` / `$31–$33`) | 1 |
| `LIO T,(R16)` / `LIO (R16),T` | `LIO T,(R16+)` / `SUB R16,1` | 2 |

**Word forms differ from `LD`.** A byte I/O access only ever moves `T`, so a full-word `LIO` cannot borrow the pointer through `EXG FT,R16` and run stepped bytes; it walks the pair pointer with two single-byte I/O accesses, an `EXG F,T` between them (which also preserves `FT` across a store), and - since the byte steps are native - no explicit `ADD`s. Word forms exist only through a **general pair** (`BC`/`DE`/`HL`) - a word form through `FT` is an operand error.

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LIO FT,(R16+)` | `LIO T,(R16+)` / `EXG F,T` / `LIO T,(R16+)` / `EXG F,T` | 4 |
| `LIO (R16+),FT` | `LIO (R16+),T` / `EXG F,T` / `LIO (R16+),T` / `EXG F,T` | 4 |
| `LIO FT,(R16)` / `LIO (R16),FT` | the stepped word form / `SUB R16,2` | 6 |
| `LIO (R16+),FT` | `LIO (R16),T` / `ADD R16,1` / `EXG F,T` / `LIO (R16),T` / `ADD R16,1` / `EXG F,T` | 6 |

## LCO with Post-increment

Supported for byte accesses on any 16-bit register pair (`FT`, `BC`, `DE`, `HL`); word accesses only through a general pair, as with `LIO`. `LCO` is load-only (no store form), so `LCO (R16),FT` and its stepped forms are operand errors. The `CODE` access-type signal is asserted through each native step. The **byte** form is native since 2026-11-13 (`$0D–$0F`); the plain spelling synthesizes with a walk-back, and the **word load** follows the `LIO` word pattern above (same instruction counts, `LCO` in place of `LIO`):

| Assembler syntax | Instruction generated | Instr |
|------------------|------------------------|-------|
| `LCO T,(R16+)` | native stepped opcode (`$0D–$0F`) | 1 |
| `LCO T,(R16)` | `LCO T,(R16+)` / `SUB R16,1` | 2 |
| `LCO FT,(R16+)` | `LCO T,(R16+)` / `EXG F,T` / `LCO T,(R16+)` / `EXG F,T` | 4 |
| `LCO FT,(R16)` | the stepped word form / `SUB R16,2` | 6 |

## Expansion cost and ranking

Every native instruction costs 4 cycles. The tables below rank the indirect/stepped access forms by how many native instructions each expands to — fastest first. The pointer you choose and whether you step matter more than you might expect.

### 16-bit access, ranked (fewest instructions first)

| Rank | Form | Instr | Cycles | Notes |
|---|------|-------|--------|-------|
| 1 | `LD FT,(R16+)` / `LD (R16+),FT` | 2 | 8 | Two native hub-byte steps — the cheapest 16-bit access |
| 2 | `LD FT,(R16)` / `LD (R16),FT` | 3 | 12 | Pair pointer, walk-back (`ADD R16,-2`) |
| 3 | `LD R16,(FT+)` / `LD (FT+),R16` | 4 | 16 | FT pointer via the `EXG FT,R16` borrow dance |
| 4 | `LD R16,(FT)` / `LD (FT),R16` | 5 | 20 | Borrow dance plus pointer restore |
| 5 | `LIO` / `LCO` word post-inc `(R16+)` | 4 | 16 | Two native stepped byte accesses plus the `EXG F,T` sandwich; word through a pair only |
| 6 | `LIO` / `LCO` word plain `(R16)` | 6 | 24 | The stepped word form plus `SUB R16,2` |

### Byte access, for reference

| Form | Instr | Cycles | Notes |
|------|-------|--------|-------|
| `LD T,(R16+)` / `LD (R16+),T` / `LD F,(R16+)` / `LD (R16+),F` | 1 | 4 | native stepped opcode |
| `LD R8,(FT+)` / `LD (FT+),R8` | 1 | 4 | native stepped opcode through the hub pointer (`R8` = B..L) |
| `LD F,(FT)` / `LD T,(FT)` | 1 | 4 | native plain — the only one-instruction plain byte accesses left |
| `LD T,(R16)` / `LD (R16),T` | 2 | 8 | synthesized: native step plus `ADD R16,-1` (the plain pair forms retired with `$01–$07`) |
| `LD R8,(FT)` / `LD (FT),R8` | 2 | 8 | synthesized: native step plus `SUB FT,1` (the plain FT forms retired 2026-11-12) |

### What surprises people

- **Post-increment is the cheapest way to step.** A byte through `BC`/`DE`/`HL` is a single native instruction (`LD T,(R16+)` = 1), and a full word is two (`LD FT,(R16+)` = 2). The plain forms must walk the pointer back — a byte costs 2 instructions (`LD T,(R16)`), a word 3 (`LD FT,(R16)`) — so when the pointer's end value is dead or compensable, `(R16+)` is both shorter and faster.
- **`BC`/`DE`/`HL` are the cheap pointers for words and `FT` is the expensive one.** A stepped word through a pair costs 2; through `FT` it costs 4, because the stepped block steps general pairs and `FT` must be borrowed through `EXG FT,R16`. For bytes the pointers now cost the same: one native instruction each way.
- **Byte plain access costs two wherever the pointer is** (stepped access plus walk-back), and stepped byte access costs one. Prefer `(FT+)`/`(R16+)` for walks; the hub bytes cannot be stepped through `FT` at all — they step only through a general pair.
- **`LIO`/`LCO` word accesses still cost more than `LD`** (4 versus 2) - a byte I/O access only ever moves `T`, so the word cannot borrow the pointer through `EXG FT,R16` - but since the byte steps went native the stepped word form is four instructions, not six.

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

There is no byte-to-byte exchange: the assembler does not synthesize one, because its expansion needed `EXG T,R8`, an opcode block that was retired from the ISA (see `ISA-V2-WISHLIST.md` A1). `EXG B,C` and friends are operand errors. A byte swap is three `LD`s through `T`, or a pair exchange when the bytes are low halves of pairs.

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
- **Post-increment:** The pointer is modified as a side effect of the synthesized sequence.

# Continue reading

[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
