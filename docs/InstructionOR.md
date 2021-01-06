# OR (8 bit, bitwise or register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01100 | r   | Not valid when r = 1 |

## Operation
```
T <- T | Reg8[r]
PC <- PC + 1
```

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
```
PC <- PC + 1
T <- T | Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
OR T,$C0
OR $04
```

