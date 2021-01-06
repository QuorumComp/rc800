# LD (Load 8 bit immediate to register)

## Opcode
| 76543 | 210 |
|-------|-----|
| 10000 | r   |

## Operation
```
PC <- PC + 1
Reg8[r] <- Code[PC]
PC <- PC + 1
```

## Assembler syntax examples
```
LD T,87
LD L,42
```

# LD (8 bit store to Data)

## Opcode
| 765432 | 10 | Notes |
|--------|----|-------|
| 000000 | r  | Not valid when r = 0 |

## Operation
```
Data[Reg16[r]] <- T
PC <- PC + 1
```

## Assembler syntax examples
```
LD (BC),T
```

# LD (8 bit load from Data)

## Opcode
| 765432 | 10 |
|--------|----|
| 000001 | r  |

## Operation
```
T <- Data[Reg16[r]]
PC <- PC + 1
```

## Assembler syntax examples
```
LD T,(BC)
```

---
# LD (8 bit, register to register)

## Opcode
| 76 | 5 | 43 | 210 | Notes |
|----|---|----|-----|-------|
| 01 | d | 11 | r   | Not valid when r = 1 |

## Operation
```
if d = 0 then
    T <- Reg8[r]
else
    Reg8[r] <- T
PC <- PC + 1
```

## Assembler syntax examples
```
LD H,T
LD T,F
```

---
# LD (16 bit, register to register)

## Opcode
| 7654 | 3 | 2 | 10 | Notes |
|------|---|---|----|-------|
| 1101 | d | 0 | r  | Not valid when r = 0 |

## Operation
```
if d = 0 then
    Reg16[r] <- FT
else
    FT <- Reg16[r]
PC <- PC + 1
```

## Assembler syntax examples
```
LD HL,FT
LD FT,BC
```

