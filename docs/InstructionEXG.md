# EXG (8 bit, exchange T and F)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 00010 | 000 | The only byte exchange; $11-$17 are illegal |

## Operation
```
Temp <- T
T <- F
F <- Temp
PC <- PC + 1
```

## Assembler syntax examples
```
EXG T,F
```

Exchanging a byte other than T or F goes through the 16 bit form below: `EXG`
of the containing pair moves both halves at once.

---
# EXG (16 bit, exchange register contents)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 110010 | r  | Not valid when r = 0 |

## Operation
```
Temp <- FT
FT <- Reg16[r]
Reg16[r] <- Temp
PC <- PC + 1
```

## Assembler syntax examples
```
EXG FT,BC
EXG HL
```
