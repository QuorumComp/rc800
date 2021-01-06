# NOT (8 bit complement)

## Opcode
| 76543210 |
|----------|
| 00011000 |

## Operation
```
F <- ~F
PC <- PC + 1
```

## Assembler syntax examples
```
NOT
NOT F
```
