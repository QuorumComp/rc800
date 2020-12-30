# OR (8 bit, bitwise or register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01100 | r   | Not valid when r = 1 |

## Operation
1. T <- T | Reg8[r]
2. PC <- PC + 1

## Assembler syntax examples
```
OR T,F
OR H
```

---
# OR (8 bit, bitwise OR immediate to register)

## Opcode
| 76543210 |
|----------|
| 10110000 |

## Operation
1. PC <- PC + 1
2. T <- T | Code[PC]
3. PC <- PC + 1

## Assembler syntax examples
```
OR T,$C0
OR $04
```

