# SYS (Perform syscall)

The SYS instruction performs a software interrupt and sets the PC to the location specified by the following byte times 8. The handle interrupt flag is set.

## Opcode
| 76543210 |
|----------|
| 10011011 |

## Operation
```
SYSF <- 1
Push (PC + 2) to register HL stack
PC <- Code[PC + 1] * 8
```

## Assembler syntax examples
```
SYS $12
```
