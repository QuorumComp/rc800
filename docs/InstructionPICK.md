# PICK (Replace top entry with specified entry at index)

`PICK` fetches an entry deeper in a register stack and places it in the register. Index zero will retrieve the register's (topmost) value, index one the value one below and so forth.

## Opcode
| 765432 | 10 |
|--------|----|    
| 000111 | r  |

## Operation
```
Reg16[r] <- Stack[r][Reg16[r] & 0xFF]
PC <- PC + 1
```

## Assembler syntax examples
```
PICK BC
PICK BC,4 (synthesized)
```
