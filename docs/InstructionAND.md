# AND (8 bit, bitwise AND register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01101 | r   | Not valid when r = 1 |

## Operation
1. T <- T & Reg8[r]
2. PC <- PC + 1

## Assembler syntax examples
```
AND T,F
AND H
```

---
# AND (8 bit, bitwise AND immediate to register)

## Opcode
| 76543210 |
|----------|
| 10110001 |

## Operation
1. PC <- PC + 1
2. T <- T & Code[PC]
3. PC <- PC + 1

## Assembler syntax examples
```
AND T,$C0
AND $04
```

