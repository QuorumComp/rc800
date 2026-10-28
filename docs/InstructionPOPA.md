# POPA (Pop all registers from register stacks)

## Opcode
| 76543210 |
|----------|    
| 11111001 |

## Operation
```
Pop FT, BC, DE, HL register stacks
PC <- PC + 1
```

Each pair pops independently at its own pointer. There is no underflow
fault: a pair whose pointer is $FF wraps to $00 and loads entry $00, as
for POP. See [POP](InstructionPOP.md).

## Assembler syntax examples
```
POPA
```
