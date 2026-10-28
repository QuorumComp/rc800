# SWAPA (Swap two topmost stack entries for all registers)

## Opcode
| 76543210 |
|----------|    
| 11001100 |

## Operation
```
Swap the two topmost stack entries for all registers
PC <- PC + 1
```

Each pair swaps at its own pointer; the boundary wrap is as for SWAP. See
[SWAP](InstructionSWAP.md).

## Assembler syntax examples
```
SWAPA
```
