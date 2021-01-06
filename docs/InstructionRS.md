# RS (16 bit, right shift register)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 11101 | r   | Not valid when r = 0 or r = 1 |

## Operation
```
FT <- FT >> (Reg8[r] & 0xF)
PC <- PC + 1
```

## Assembler syntax examples
```
RS FT,B
RS C
```

---
# RS (16 bit, right shift register)

## Opcode
| 76543210 |
|----------|
| 10111001 |

## Operation
```
PC <- PC + 1
FT <- FT >> (Code[PC] & 0xF)
PC <- PC + 1
```

## Assembler syntax examples
```
RS 4
RS FT,5
```


