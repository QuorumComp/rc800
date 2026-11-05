# PICK (Replace top entry with the entry at an immediate index)

`PICK` fetches an entry deeper in a register stack and places it in the register. Index zero retrieves the register's (topmost) value, index one the value one below and so forth.

The index is the instruction's immediate operand byte. Index 0 leaves the register unchanged, and the stack pointer is not touched, so the depth of the stack is the same before and after the instruction. `T` — and the whole T/FT hub — survive the access whatever the target stack.

## Opcode
| 765432 | 10 |
|--------|----|
| 101111 | r  |

Followed by the index byte `i8`. `r = 0` is `FT`, `r = 1` is `BC`, `r = 2` is `DE`, `r = 3` is `HL`.

## Operation
```
PC <- PC + 1
Reg16[r] <- Stack[r][Code[PC]]
PC <- PC + 1
```

## Notes

Only the named register pair is written; no other register, flag or stack pointer is touched. An index past the current stack depth reads an entry that has not been written since the stack pointer passed it; it does not trap. The address is `SP + i8` computed modulo 256, so a large index wraps around the ring of cells: `PICK` never faults, at any depth.

## Assembler syntax examples
```
PICK BC,4           ; BC = entry 4 of the BC stack
PICK FT,0           ; no-op (index 0 is the register itself)
```

`PICK` and `STOW` together form a register-stack local frame; see [STOW](InstructionSTOW.md) and [Local Variables](StandardCallingConvention.md#local-variables).
