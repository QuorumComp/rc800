# JAL (jump and link return address)

## Opcode
| 765432 | 10 |
|--------|----|
| 001110 | r  |

## Operation
```
TEMP <- PC + 1
PC <- Reg16[r]
HL <- TEMP
```

## Assembler syntax examples
```
JAL (BC)
JAL (HL)
```
