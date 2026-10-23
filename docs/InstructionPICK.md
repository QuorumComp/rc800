# PICK (Replace top entry with specified entry at index)

`PICK` fetches an entry deeper in a register stack and places it in the register. Index zero will retrieve the register's (topmost) value, index one the value one below and so forth.

The index is taken from `T`, whatever the target register stack is. Index 0 leaves the register unchanged, and the stack pointer is not touched, so the depth of the stack is the same before and after the instruction.

## Opcode
| 765432 | 10 |
|--------|----|    
| 000111 | r  |

## Operation
```
Reg16[r] <- Stack[r][T]
PC <- PC + 1
```

## Notes

`T` is read, not written, and the named register pair is the only register written. Since loading the index with `LD T,n` writes the FT pair — and the FT stack top is the same storage — park FT around the index load when its value is live:

```
PUSH FT / LD T,E / PICK BC / POP FT
```

An index past the current stack depth reads an entry that has not been written since the stack pointer passed it; it does not trap.

## Assembler syntax examples
```
LD T,4
PICK BC              ; BC = entry 4 of the BC stack
PICK FT              ; index already in T
PICK FT,4            (synthesized: LD T,4 / PICK FT)
```

`PICK FT,i8` is the only synthesized immediate form. For BC, DE and HL the assembler rejects an immediate index, because the implicit `LD T,i8` would clobber FT.
