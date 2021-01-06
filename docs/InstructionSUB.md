# SUB (8 bit, register to register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 01010 | r   | Not valid when r = 1 |

## Operation
```
T <- T - Reg8[r]
PC <- PC + 1
```

## Assembler syntax examples
```
SUB T,F
SUB C
```

---
# SUB (16 bit, register to register)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 111100 | r  | Not valid when r = 0 |

## Operation
```
FT <- FT - Reg16[r]
PC <- PC + 1
```

## Assembler syntax examples
```
SUB FT,BC
SUB HL
```
