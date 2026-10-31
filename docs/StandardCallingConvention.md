# Standard Calling Convention

This is the standard calling convention for the RC800 family.

## Notation

As each machine register is a stack, and parameters may be passed on these stacks, stack values are represented by adding apostrophes to the register mnemonic.

`FT` refers to the current (topmost) value of the `FT` register.

`FT'` is the next value on the stack, the one that will appear after a `POP FT` instruction.

`FT''` is the next value again, and so forth.

`R` may refer to any 16-bit register.

`multi-word` refers to a value or object larger than 16 bits.

## Parameter Passing

Parameters are assigned left to right. The first parameters use registers from the sequence **T, B, C, D, E** (8-bit) or **FT, BC, DE** (16-bit). When the register sequence is exhausted, remaining parameters spill onto the HL stack. `HL` is never used for register parameters because `jal` overwrites it with the return address.

- **8-bit param** — occupies one register from the sequence, or one 16-bit word on the HL stack (value in low byte, high byte sign- or zero-extended per declared type)
- **16-bit param** — occupies FT, BC, or DE in order, or one 16-bit word on the HL stack
- **Multi-word (≥32-bit)** — passed on a register stack; MSW in R, next word in R'

Mixed 8-bit and 16-bit parameters are allowed. An 8-bit parameter may not use a register that is part of an already-claimed 16-bit pair. Organize parameter order to avoid wasting registers or leaving "holes" in the assignment sequence.

### Examples

| Signature | Assignment |
|-----------|-----------|
| `f(u8 a, u8 b)` | T, B |
| `f(u16 a, u16 b)` | FT, BC |
| `f(u16 a, u8 b)` | FT, B |
| `f(u8 a, u16 b)` | T, BC |
| `f(u8 a, u16 b, u8 c)` | T, BC, D |
| `f(u8 a, u8 b, u16 c)` | T, B, —, DE (hole at C — avoid) |
| `f(u8 a, u8 b, u8 c, s8 d, u8 e)` | T, B, C, D, E |
| `f(u16 a, u16 b, u16 c)` | FT, BC, DE |

When the first parameter is 8-bit (occupying T), FT is unavailable because T is the low byte of FT, so the first 16-bit parameter falls back to BC. Put 16-bit parameters first in the signature to give them access to FT: prefer `f(u16, u8)` → FT, B over `f(u8, u16)` → T, BC.

## Stack Parameters

Parameters that do not fit in registers are passed on the HL stack. Each parameter occupies one 16-bit word regardless of size. For 8-bit parameters, the value is in the low byte; the high byte is sign-extended for signed types (`s8`) and zero-extended for unsigned types (`u8`).

### Caller Responsibilities

1. Push stack parameters onto the HL stack in **reverse declaration order** (first stack param pushed last), so the first param ends up nearest the sacrificial slot.
2. Push a sacrificial slot after the last parameter — `jal` overwrites the HL stack top with the return address.
3. Load register parameters into their assigned registers.

```
        ; call f(u8 a, u16 b, u16 c, s8 d, u8 e)
        ; registers: T=a, BC=b, DE=c  |  stack: d(s8), e(u8)
        ld      t,arg_e
        ld      f,0            ; zero-extend (u8)
        ld      hl,ft
        push    hl             ; e on HL stack (last param pushed first)
        ld      t,arg_d
        ext                    ; sign-extend (s8)
        ld      hl,ft
        push    hl             ; d on HL stack (first param pushed last)
        push    hl             ; sacrificial slot for jal
        ld      t,arg_a
        ld      bc,arg_b
        ld      ft,arg_c
        ld      de,ft
        jal     f
```

### Stack Layout After `jal`

```
  HL (index 0, SP)   = return address
  index 1             = d (first stack param, pushed last)
  index 2             = e (last stack param, pushed first)
```

The first stack parameter is closest to the return address (index 1); later parameters sit at ever-higher indices, so a callee consumes them in declaration order.

### Callee Responsibilities

There are two strategies for accessing stack parameters. The callee should employ whichever fits best, or combine them.

#### Strategy 1 — `PICK` (params stay on stack)

`PUSH HL` duplicates the return address on the HL stack, shifting params to higher indices. `LD T,$nn` followed by `PICK HL` reads a parameter at a fixed index without disturbing the return address. Parameters remain on the stack and can be re-read at any time — useful when a param is accessed multiple times or conditionally.

```
        push    hl             ; dup ret; params shift to index 2+
        ld      t,2            ; index
        pick    hl             ; HL=arg1 (first stack param)
        ld      t,l            ; T=arg1 low byte
        ld      t,3            ; index
        pick    hl             ; HL=arg2
        ld      t,l
        ; ... params remain on stack for later access ...
```

The index lives in `T`, so the `LD T,$nn` clobbers FT — and the FT stack top, which is the same storage. That costs nothing here: FT is caller-saved and the parameter is read out of HL immediately. Park FT around the index load when a live FT-stack value (a 32-bit `FT:FT'` pair) has to survive.

#### Strategy 2 — swap-pop (params consumed as accessed)

No `PUSH HL` needed. `SWAP HL` exchanges HL with `Stack[SP+1]`, placing the parameter in HL and the return address into `Stack[SP+1]`. The parameter in HL can be used directly as an operand (`ADD T,L`, `ADD FT,HL`, `LD (BC),L`, etc.) — no register save needed when the value is consumed immediately. `POP HL` advances SP and reloads HL from `Stack[SP]` (which now holds the return address). Params are discarded as they are consumed — the stack is cleaned up incrementally.

```
        swap    hl             ; HL=arg1 (closest to ret)
        add     ft,hl          ; use HL directly as operand
        pop     hl             ; HL=ret restored, arg1 discarded

        swap    hl             ; HL=arg2
        add     t,l            ; use L directly
        pop     hl             ; HL=ret restored, arg2 discarded
        ; ... stack is clean, no further cleanup needed ...
```

When a parameter must be reused across multiple operations, save it to a callee-saved register byte between swap and pop:

```
        swap    hl             ; HL=arg
        ld      t,l            ; T=arg low byte
        ld      d,t            ; D=arg (saved for later use)
        pop     hl             ; HL=ret restored
```

**Combining strategies:** a function might `PICK` a frequently-accessed parameter (like a loop bound or pointer) and swap-pop the rest. The `PUSH HL` is only needed if `PICK` is used.

### Stack Cleanup

After accessing parameters, any remaining entries on the HL stack must be discarded. If there are just a few entries, individual `POP HL` instructions are fine. For many entries, use `LCR` to advance the HL stack pointer in bulk:

```
        ; Discard N+1 entries (N stack params + 1 sacrificial slot)
        ld      c,RC8_SP_HL
        lcr     t,(c)          ; T = SP_HL
        add     t,N+1          ; advance past remaining entries
        lcr     (c),t          ; SP_HL = new value
        ; HL = Stack[new_SP] = return address (if aligned correctly)
```

### Local Variables

A callee can keep locals on a register stack and update them in place with `PICK` and `STOW`; the stack pointer does not move. Use BC or DE for the frame — HL carries the return address, and there is no `STOW FT`. The register is the scratch slot at depth 0, the locals are at depths 1..N, and the caller's saved pair is at depth N+1.

```
        ; two 16-bit locals on the DE stack
        push    de              ; save caller DE
        ld      de,0            ; clear the duplicate: depth 0 = scratch
        push    de              ; allocate a zero local
        push    de              ; allocate a zero local
        ; depth 0 = scratch, 1 = local 1, 2 = local 2, 3 = caller DE

        ld      t,2
        pick    de
        add     de,4
        stow    de              ; local 2 += 4

        ld      t,1
        pick    de
        exg     ft,de           ; FT = local, DE = FT before the exchange
        ls      ft,1
        add     ft,bc
        exg     ft,de           ; FT restored, T = 1, DE = result
        stow    de              ; local 1 = (local 1 << 1) + BC

        pop     de              ; discard scratch
        pop     de              ; discard local 1
        pop     de              ; discard local 2, DE = caller DE
```

`PICK` and `STOW` leave T unchanged, and the native `ADD R16,s8` (or the synthesized `ADD R16,i16`) on the frame pair does not write T, so one index load covers a simple update. The double `EXG` restores FT — including T, so the `STOW` targets the same depth — and leaves the result in DE. It does not undo the FT clobber from `LD T,n`; park FT around the index load when its value must survive, and keep the work between the exchanges register-only. The other pair holds the pre-exchange FT, so leave it untouched until the second exchange; a `PUSH`/`POP FT` there also changes the FT stack pointer.

For N locals the frame uses N+2 entries. For a large frame, restore SP with `LCR`; park the return value in FT first because the index load clobbers it. The pop-based epilogue takes N+1 pops: the scratch slot and the N locals, then the saved pair.

### Epilogue — Saving the Return Value

The callee computes its result in FT. The epilogue saves the result on the FT stack, puts the return address into FT for HL restoration, cleans up the HL stack, then recovers the result:

```
        ; FT = result, HL = ret
        push    ft             ; dup result on FT stack
        ld      ft,hl          ; FT=ret (overwrites dup; original result preserved below)
        pop     de/bc          ; restore callee-saved
        pop     hl             ; discard sacrificial slot
        ld      hl,ft          ; HL=ret
        pop     ft             ; FT=result (restored from FT stack)
        j       (hl)
```

`PUSH FT` duplicates the result. `LD FT,HL` overwrites the duplicate with the return address while the original result survives at `Stack[SP_FT+1]`. `POP FT` at the end restores it.

### Nested Calls

When the callee makes a nested call, the inner callee's HL stack cleanup will discard outer stack entries. Save the return address in FT before the inner call:

```
        push    ft             ; save FT
        ld      ft,hl          ; FT=ret
        ; ... set up inner call, jal inner ...
        pop     hl             ; discard inner args + sacrificial
        ld      hl,ft          ; restore outer ret
        pop     ft             ; restore FT
```

### Prologue/Epilogue Templates

**Functions with stack parameters** (swap-pop variant):

```
f:
        push    bc/de          ; callee-saved (NOT hl)

        ; swap-pop each stack param
        swap    hl; ld t,l; pop hl
        ; ... body: compute result in FT ...

        push    ft             ; save result
        ld      ft,hl          ; FT=ret
        pop     de/bc          ; restore callee-saved
        pop     hl             ; discard sacrificial slot
        ld      hl,ft          ; HL=ret
        pop     ft             ; FT=result
        j       (hl)
```

**Functions with stack parameters** (`PICK` variant):

```
f:
        push    hl             ; save ret on HL stack
        push    bc/de          ; callee-saved

        ; PICK each stack param at fixed index (index in T, clobbers FT)
        ld      t,2            ; index
        pick    hl             ; HL=arg1
        ; ... body: compute result in FT ...

        push    ft             ; save result
        ld      ft,hl          ; FT=ret
        pop     de/bc          ; restore callee-saved
        ; discard remaining HL stack entries (pops or lcr)
        pop     hl             ; discard ret dup
        pop     hl             ; discard argN
        pop     hl             ; discard ret (saved in FT)
        ld      hl,ft          ; HL=ret
        pop     ft             ; FT=result
        j       (hl)
```

**Functions without stack parameters** (standard):

```
f:
        push    bc/de/hl
        ; ... body ...
        pop     hl/de/bc
        j       (hl)
```

## Returning Values

- Returned in FT (or on FT stack for multi-word: the same word ladder, MSW left in the register, remaining words pushed LSW-first; the caller pops W-1 cells).
- **8-bit returns:** When returning an 8-bit value in T, there is no need to clear F. The caller reads only T for an 8-bit result, so the contents of F are irrelevant. Only clear F when the return value is genuinely 16-bit and the high byte must be zero.

## Passing Multi-word Values

Parameters 32 bits or larger can be passed on one register stack. In the case of 32-bit integers, `R` and `R'` hold the two 16-bit words, the most significant word in `R`.

The general form is a **word ladder**: a value of `W` words occupies the register and the `W-1` cells below it, most-significant word in the register, next word at `R'`, and so on down to the least-significant word at `R^(W-1)`. The caller builds the ladder by pushing the words least-significant first and leaving the most-significant word in the register:

```
        ; call f(64-bit v): caller side
        ld      ft, v+6        ; LSW
        push    ft
        ld      ft, v+4
        push    ft
        ld      ft, v+2
        push    ft
        ld      ft, v          ; MSW stays in the register
        ld      h,#>_f
        ld      l,#<_f
        jal     (hl)
```

The callee reads word `k` with `ld t,#k` / `pick R` (index 0 is the register itself) and **consumes the ladder by popping `W-1` cells** — one less word than the value uses, because the register word is not a cell. This keeps the stack balanced whatever the value's width: the cell under the ladder belongs to the caller.

Multi-word **return values** use the same ladder, MSW topmost: the callee pushes the words below the most-significant one least-significant first and leaves the MSW in the register; the caller copies the words out and pops `W-1` cells.

The standard route for multi-word parameters is the `FT` stack (`jal` clobbers HL, and BC/DE have no `PICK`, so a second multi-word parameter has no ladder reader; ports pass it on the HL stack as plain cells instead).

## Preserved Registers

The convention is generally "callee-saves".

- **Callee-saves:** BC, DE (including stack values).
- **Caller-saves:** FT (always assumed destroyed by callee).
- **HL:** holds the return address set by `jal`. Management depends on the function type:
  - **No stack parameters:** standard `push bc/de/hl` / `pop hl/de/bc`
  - **Swap-pop variant:** prologue pushes only `bc/de` (not HL). Params are consumed via `swap hl` / `pop hl`, which naturally preserves HL=ret between each pair. Epilogue `pop hl` discards the sacrificial slot.
  - **PICK variant:** prologue `push hl` duplicates ret (shifting params to higher indices). Epilogue discards all remaining HL stack entries (ret dup, ret, and all params) via individual `pop hl` or `LCR` advance.

Consuming a multi-word parameter passed on the `FT` stack follows the rule in [Passing Multi-word Values](#passing-multi-word-values): pop one less word than the value uses.

## Subroutines

- `JAL (r16)` copies the return address into HL (does NOT push).
- Non-leaf subroutines must save/restore HL themselves.
- Return via `J (HL)`.

## Prologue/Epilogue Optimization

Use register-list `push`/`pop` forms in function prologues and epilogues (`push bc/de/hl`, `pop hl/de/bc`, etc.). The assembler can choose compact synthesized sequences automatically.

- `pusha`/`popa` execute in the same time as a single-register `push`/`pop`.
- For procedures that do not return a value in FT, `pusha`/`popa` is usually the fastest full-save prologue/epilogue.
- Any save set can be expressed in at most two instructions: either individual pushes/pops (for 1-2 registers), or `pusha`/`popa` plus one compensating `pop`/`push` (for 3 registers).

**Functions with stack parameters** use a different prologue/epilogue. See [Stack Parameters](#stack-parameters) for details. The key difference: HL is not part of the standard `push bc/de/hl` save set — it is managed through the HL stack.

# Continue reading

[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
