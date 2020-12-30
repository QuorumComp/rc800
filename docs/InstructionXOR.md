# XOR (8 bit, bitwise xor register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01110 | r   | Not valid when r = 1 |

## Operation
1. T <- T ^ Reg8[r]
2. PC <- PC + 1

## Assembler syntax examples
```
XOR T,F
XOR H
```

