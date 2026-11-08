# LD (Load 8 bit immediate to register)

## Opcode
| 76543 | 210 |
|-------|-----|
| 10000 | r   |

## Operation
```
PC <- PC + 1
Reg8[r] <- Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
LD T,87
LD L,42
```

# LD (8 bit store to Data) — retired

The plain pair stores `LD (BC),T` / `LD (DE),T` / `LD (HL),T` (`$01–$03`) were
retired 2026-11-12: those values now take the illegal-instruction vector. The
assembler still accepts the syntax and synthesizes a stepped store plus a
rewind:

```
LD (HL),T   ->   LD (HL+),T      ; $13
                ADD HL,-1       ; $9F $FF
```

# LD (8 bit load from Data via R16) — retired

The plain pair loads `LD T,(BC)` / `LD T,(DE)` / `LD T,(HL)` (`$05–$07`) were
retired 2026-11-12 along with the plain stores `$01–$03`: those values now take
the illegal-instruction vector. The assembler still accepts the syntax and
synthesizes a stepped load plus a rewind:

```
LD T,(HL)   ->   LD T,(HL+)      ; $17
                ADD HL,-1       ; $9F $FF
```

`LD T,(FT)` held `$04` until the same day, when the plain pair accesses left the
encoding altogether. It moved to `$29`, the `r=1` cell of `LD r8,(FT)` below,
which names `T` — the load itself stays native because its stepped form cannot
exist: stepping FT would clobber the byte being moved. On 2026-11-12 the whole
FT row gained the step and the hub-byte loads moved to an interim home at
`$08`/`$09`; on 2026-11-13 they came back to `$28`/`$29`, the cells of their own
load row, as its `r=0`/`r=1` exception cells (see below). A stale `$04` is
illegal by explicit mask rather than an unclaimed hole, so an old binary traps
instead of silently skipping the load. A stale `$08`/`$09` traps the same way —
those cells went back to being deliberately illegal with the move home.

---
# LD (8 bit store to Data via FT, post-increment)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 00100 | r   | `$22–$27`; not valid when r = 0 or r = 1 |

## Operation
```
Data[FT] <- Reg8[r]
FT <- FT + 1
PC <- PC + 1
```

## Assembler syntax examples
```
LD (FT+),B
LD (FT+),L
```

The store steps FT: the byte lands at the address FT held when the instruction
began — the memory stage reads its address before the stepped pointer is
written — and FT leaves one higher. Only `CMP` writes flags, so the step leaves
them alone, and the FT stack-top cell follows the pointer like any pair write.
`r = 0` and `r = 1` (`$20`, `$21`) are illegal: `F` and `T` are halves of FT, and
the stored byte would land inside the pointer being stepped. The plain spelling
`LD (FT),B` is assembler synthesis — the stepped store plus `SUB FT,1` — see
[SynthesizedInstructions](SynthesizedInstructions.md).

---
# LD (8 bit load from Data via FT, post-increment)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 00101 | r   | `$2A–$2F`; not valid when r = 0 or r = 1 |

## Operation
```
Reg8[r] <- Data[FT]
FT <- FT + 1
PC <- PC + 1
```

## Assembler syntax examples
```
LD B,(FT+)
LD L,(FT+)
```

Two registers move: the loaded byte arrives at `r` on the auxiliary write port
while the main port steps FT. As with the store, `r = 0` and `r = 1` are not
stepping cells: those two — `$28`/`$29` — are the hub-byte loads below, the
cells whose byte cannot ride the stepped pointer. The plain spelling
`LD B,(FT)` is assembler synthesis: the stepped load plus `SUB FT,1`.

---
# LD (hub byte load from Data via FT)

## Opcode
| 7654321 | 0 | Notes |
|---------|---|-------|
| 0010100 | 0 | `$28`: `LD F,(FT)` |
| 0010100 | 1 | `$29`: `LD T,(FT)` |

## Operation
```
F <- Data[FT]      ; or T, for $29
PC <- PC + 1
```

## Assembler syntax examples
```
LD F,(FT)
LD T,(FT)
```

The only byte loads that do not step, because they cannot: the loaded byte is a
half of the very pointer a step would install, so the step would overwrite what
it delivered. They are the `r=0`/`r=1` exception cells of the `LD r8,(FT+)` row
at their home `$28`/`$29` since 2026-11-13; the interim home at `$08`/`$09` (the
cells `EI`/`DI` had vacated - see [EI](InstructionEI.md) and
[DI](InstructionDI.md)) went back to being deliberately illegal with the move,
so a stale binary there traps. They are also the only way
the flag byte `F` can be loaded from memory. Note that the write itself changes
FT (that is the point), so the pointer read through is gone either way; what
stands still is the step.

---
# LD (8 bit stepped access to Data, post-increment)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 000100 | r  | Not valid when r = 0; $10 is EXG T,F |
| 000101 | r  | Not valid when r = 0 |
| 000110 | r  | Not valid when r = 0 |
| 000111 | r  | Not valid when r = 0 |

## Operation
```
; load  ($15-$17 load T, $1D-$1F load F)
T <- Data[Reg16[r]]        ; or F, for the $1D-$1F opcodes
Reg16[r] <- Reg16[r] + 1

; store ($11-$13 store T, $19-$1B store F)
Data[Reg16[r]] <- T        ; or F, for the $19-$1B opcodes
Reg16[r] <- Reg16[r] + 1

PC <- PC + 1
```

## Assembler syntax examples
```
LD T,(HL+)
LD F,(DE+)
LD (BC+),T
LD (HL+),F
```

The byte moved is always a hub byte — `T` or `F` — and the pointer stepped is
always a general pair: `r = 0` names `FT`, whose three cells `$14`, `$18` and
`$1C` are illegal and take the illegal-instruction fault. That is not a missing
encoding but a property of the operation: `T` and `F` are the two halves of `FT`,
so a hub byte through an `FT` pointer writes half of the very pointer it would
advance. `$10` is not free either — it is `EXG T,F`.

A stepped access advances its pointer **by the width of the access**: 1 here, 2
for a 16-bit access. The 16-bit stepped forms (`LD FT,(R16+)` and friends) are
not native; the assembler expands them, and
[SynthesizedInstructions](SynthesizedInstructions.md) gives the
expansions and their costs. Pre-decrement is retired outright — stepped
accesses only step up, and `(-R16)` forms are assembler errors.
Through a general pair only the hub-byte step shown above is a single
instruction — `LD C,(HL+)` does not exist, so step through `T` and transfer with
`LD R8,T`. Through the hub pointer it is the other way round: the byte-register
forms [`LD (FT+),r8` / `LD r8,(FT+)`](#ld-8-bit-store-to-data-via-ft-post-increment)
are native and the hub-byte step is not.

The `T` forms leave the condition codes alone — nothing in an `LD` sets flags
implicitly (only `CMP` and `TST` write `F`), and the pointer bump is part of the
instruction rather than an `ADD`. The `F` forms are a different matter: `F` *is*
the flags, so `LD F,(R16+)` **replaces the condition codes** with the byte loaded
and `LD (R16+),F` reads them. That is the documented way to spill and refill a
flag state, and it is what the carry-lane idiom relies on. It also means a 16-bit
stepped access — `LD FT,(R16+)`, one `T` step and one `F` step — overwrites the
flags by construction, because `F` is the high half of `FT`.

---
# LD (8 bit, register to register)

## Opcode
| 76 | 5 | 43 | 210 | Notes |
|----|---|----|-----|-------|
| 01 | d | 11 | r   | Not valid when r = 1 |

## Operation
```
if d = 0 then
    T <- Reg8[r]
else
    Reg8[r] <- T
PC <- PC + 1
```

## Assembler syntax examples
```
LD H,T
LD T,F
```

---
# LD (16 bit, register to register)

## Opcode
| 7654 | 3 | 2 | 10 | Notes |
|------|---|---|----|-------|
| 1101 | d | 0 | r  | Not valid when r = 0 |

## Operation
```
if d = 0 then
    Reg16[r] <- FT
else
    FT <- Reg16[r]
PC <- PC + 1
```

## Assembler syntax examples
```
LD HL,FT
LD FT,BC
```

