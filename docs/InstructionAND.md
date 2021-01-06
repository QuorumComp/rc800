# AND (8 bit, bitwise AND register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01101 | r   | Not valid when r = 1 |

## Operation
```
T <- T & Reg8[r]
PC <- PC + 1
```

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
```
PC <- PC + 1
T <- T & Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
AND T,$C0
AND $04
```

