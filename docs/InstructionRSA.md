# RSA (16 bit, right shift register arithmetic)

## Opcode
| 76543 | 210 | Notes |
|-------|-----|-------|
| 11111 | r   | Not valid when r = 0 or r = 1 |

## Operation
```
FT <- FT >>> (Reg8[r] & 0xF)
PC <- PC + 1
```

## Assembler syntax examples
```
RSA FT,B
RSA C
```

---
# RSA (16 bit, right shift register arithmetic)

## Opcode
| 76543210 |
|----------|
| 10111011 |

## Operation
```
PC <- PC + 1
FT <- FT >>> (Code[PC] & 0xF)
PC <- PC + 1
```

## Assembler syntax examples
```
RSA 4
RSA FT,5
```


