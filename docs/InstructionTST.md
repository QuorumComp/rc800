# TST (16 bit, compare register pair with zero)

The SYS instruction performs a software interrupt and sets the PC to the location specified by the following byte times 8. The handle interrupt flag is set.

## Opcode
| 765432 | 10 |
|--------|----|
| 110101 | r  |

## Operation
```
F <- Flags[Reg16[r] - 0]
PC <- PC + 1
```

## Assembler syntax examples
```
TST	BC
TST	DE
```
