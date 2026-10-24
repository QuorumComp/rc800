# STOW (Store register into the stack entry at a specified index)

`STOW` writes a register into an entry deeper in its own register stack. Index zero writes the register's (topmost) entry, which is a no-op; index one writes the entry below it, and so forth.

The index is taken from `T`, whatever the target register stack is. The stack pointer is not touched and the register is unchanged, so the depth of the stack is the same before and after the instruction.

## Opcode
| 765432 | 10 |
|--------|----|
| 000110 | r  |

`r = 1` is `BC`, `r = 2` is `DE`, `r = 3` is `HL`. `r = 0` (`STOW FT`, opcode `$18`) is illegal: `T` is both the index and the low byte of `FT`, so the value and the index cannot be independent.

## Operation
```
Stack[r][T] <- Reg16[r]
PC <- PC + 1
```

## Assembler syntax examples
```
LD T,4
STOW BC              ; BC = entry 4 of the BC stack

LD T,1
STOW DE              ; DE = entry 1 of the DE stack
```
