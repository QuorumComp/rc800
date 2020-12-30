# JAL (jump and link return address)

## Opcode
| 765432 | 10 |
|--------|----|
| 001110 | r  |

## Operation
1. TEMP <- PC + 1
2. PC <- Reg16[r]
3. HL <- TEMP

## Assembler syntax examples
```
JAL (BC)
JAL (HL)
```
