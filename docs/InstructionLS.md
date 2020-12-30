# LS (16 bit, left shift register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 11100 | r   | Not valid when r = 0 or r = 1 |

## Operation
1. FT <- FT << (Reg8[r] & 0xF)
2. PC <- PC + 1

## Assembler syntax examples
```
LS FT,B
LS C
```

---
# LS (16 bit, left shift register)

## Opcode
| 76543210 |
|----------|
| 10111000 |

## Operation
1. PC <- PC + 1
2. FT <- FT << (Code[PC] & 0xF)
3. PC <- PC + 1

## Assembler syntax examples
```
LS 4
LS FT,5
```


