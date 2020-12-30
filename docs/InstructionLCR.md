# LCR (8 bit, load configuration register)

## Opcode
| 7654321 | 0 |
|---------|---|
| 0000101 | d |

## Operation
```
1. if d = 0 then
     CR[C] <- T
   else
     T <- CR[C]
2. PC <- PC + 1
```

## Assembler syntax examples
```
LCR (C),T
LCR T,(C)
```
