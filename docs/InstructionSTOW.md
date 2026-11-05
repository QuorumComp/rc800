# STOW (Store register into the stack entry at an immediate index)

`STOW` writes a register into an entry deeper in its own register stack. Index zero writes the register's (topmost) entry, which is a no-op; index one writes the entry below it, and so forth.

The index is the instruction's immediate operand byte. The stack pointer is not touched and the register is unchanged, so the depth of the stack is the same before and after the instruction. `T` — and the whole T/FT hub — survive the access whatever the target stack, so `STOW FT,i` is legal.

## Opcode
| 765432 | 10 |
|--------|----|
| 101101 | r  |

Followed by the index byte `i8`. `r = 0` is `FT`, `r = 1` is `BC`, `r = 2` is `DE`, `r = 3` is `HL`.

## Operation
```
PC <- PC + 1
Stack[r][Code[PC]] <- Reg16[r]
PC <- PC + 1
```

## Notes

No bounds check is performed: the target is `SP + i8` computed modulo 256, so a
large index wraps around the ring of cells and a deep `STOW` can overwrite the
caller's saved pair or the entries above the current pointer. `STOW` never
faults.

## Assembler syntax examples
```
STOW BC,3           ; Stack[BC][$FF+3] <- BC
STOW HL,0           ; no-op (index 0 is the register itself)
```

## Local variables

`PICK`/`STOW` with immediate indices give a register stack the frame-relative
locals a memory stack gets from an index register, without leaving the stack:

```
        ; entry: FT holds a value to keep across the call as a local
        push    ft              ; open a cell for the local
        ...                     ; FT now holds call scratch
        pick    ft,1           ; read the local back
        stow    ft,2           ; write two cells down, leave the top alone
```

Because the index is an immediate, a fixed frame slot is one instruction; the
access needs no scratch register, so a local read or write clobbers nothing —
including for FT itself, which the old `T`-indexed form could not express.

