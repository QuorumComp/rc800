# LCR (8 bit, load configuration register)

## Opcode
| 7654321 | 0 |
|---------|---|
| 0000101 | d |

## Operation
```
if d = 0 then
    CR[C] <- T
else
    T <- CR[C]
PC <- PC + 1
```

## Notes

Indices `$00` to `$03` address the FT, BC, DE and HL stack pointers (see
[configuration registers](ConfigurationRegisters.md)).

Writing a stack pointer moves the window rather than copying a value: the
target register pair becomes the entry at the new stack pointer and does not
retain its previous value. Rewinding therefore exposes entries that were pushed
earlier. An entry that the stack pointer has never reached reads as undefined
(the stack RAM is not initialised at power-up), the same rule as
[PICK](InstructionPICK.md).

Reading a stack pointer leaves the register pairs unchanged.

## Assembler syntax examples
```
LCR (C),T
LCR T,(C)
```
