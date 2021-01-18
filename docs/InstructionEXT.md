# EXT (Sign extend T)

## Opcode
| 76543210 |
|----------|
| 01001001 |

## Operation
```
F <- if T[7] = 1 then $FF else $00
PC <- PC + 1
```

## Assembler syntax examples
```
EXT
```
