# Instruction groups

## Legend
| Symbol | Meaning |
|--------|---------|
| R8     | 8 bit register |
| R16    | 16 bit register |
| i8     | 8 bit value |
| s8     | Signed 8 bit value |
| i16    | 16 bit value |
| cc     | Condition code |
| Rlist  | Register list, such as BC/DE (BC and DE registers), BC-DL (BC, DE and HL), or a combination thereof |

## Condition codes
| Code | Meaning | Alternative |
|------|---------|---------|
| LE   | Signed less than or equal ||
| GT   | Signed greater than ||
| LT   | Signed less than ||
| GE   | Signed greater than or equal ||
| LEU  | Unsigned less than or equal ||
| GTU  | Unsigned greater than ||
| LTU  | Unsigned less than | C (carry set) |
| GEU  | Unsigned greater than or equal | not C (carry clear) |
| EQ   | Equal | Z (zero) |
| NE   | Not equal | NZ (not zero) |


## Synthesized instructions
For improved quality of life, the assembler provides several synthesized instructions. This includes instructions such as ```LD FT,Label``` which would be cumbersome to write manually every time the programmer needed to load the address of a label. These instructions are marked as ```(synthesized)``` in the tables below.

All mnemonics may be followed by a condition code. The assembler will synthesize this into a ```J/CC``` (with the opposite condition CC) and the instruction.

For instance ```LD/EQ T,1``` will load T with the value 1 if the flags in F satisfy the EQ condition.

The `!CC` is the inverse condition (for example, `EQ` → `NE`, `LT` → `GE`). `J/CC` itself is native — it is the primitive used to synthesize these forms. Any instruction can be made conditional, including `RETI/CC`.


## Arithmetic
| Mnemonic         | Operation | Remarks |
|------------------|-----------|---------|
| ADD&nbsp;T,R8    | Add 8 bit register to T ||
| ADD&nbsp;FT,R16  | Add 16 bit register to FT ||
| ADD&nbsp;R8,i8   | Add immediate byte to 8 bit register ||
| ADD&nbsp;R16,s8  | Add immediate signed byte to 16 bit register ||
| ADD&nbsp;R16,i16 | Add immediate 16 bit value to 16 bit register | (synthesized) |
| SUB&nbsp;T,R8    | Subtract 8 bit register from T ||
| SUB&nbsp;FT,R16  | Subtract 16 bit register from FT ||
| SUB&nbsp;R8,i8   | Subtract immediate byte from 8 bit register | (synthesized) |
| SUB&nbsp;R16,s8  | Subtract immediate signed byte from 16 bit register | (synthesized) |
| SUB&nbsp;R16,i16 | Subtract immediate 16 bit value from 16 bit register | (synthesized) |
| EXT&nbsp;        | Sign extend T register ||
| NEG&nbsp;T       | Negate T register ||
| NEG&nbsp;FT      | Negate FT register ||

## Bitwise
| Mnemonic       | Operation | Remarks |
|----------------|-----------|---------|
| AND&nbsp;T,R8  | Bitwise AND T with 8 bit register ||
| AND&nbsp;T,i8  | Bitwise AND T register with immediate byte ||
| OR&nbsp;T,R8   | Bitwise OR T with 8 bit register ||
| OR&nbsp;T,i8   | Bitwise OR T register with immediate byte ||
| XOR&nbsp;T,R8  | Bitwise XOR T with 8 bit register ||
| XOR&nbsp;T,i8  | Bitwise XOR T register with immediate byte ||
| LS&nbsp;FT,R8  | Left shift register FT by amount in 8 bit register ||
| LS&nbsp;FT,i8  | Left shift register FT by immediate amount ||
| RS&nbsp;FT,R8  | Right shift register FT by amount in 8 bit register ||
| RS&nbsp;FT,i8  | Right shift register FT by immediate amount ||
| RSA&nbsp;FT,R8 | Arithmetically right shift register FT by amount in 8 bit register ||
| RSA&nbsp;FT,i8 | Arithmetically right shift register FT by immediate amount ||
| NOT&nbsp;F     | Set F to one's complement of itself ||
| NOT&nbsp;T     | Set T to one's complement of itself | (synthesized) |
| NOT&nbsp;FT    | Set FT to one's complement of itself | (synthesized) |

## Comparison
| Mnemonic        | Operation | Remarks |
|-----------------|-----------|---------|
| CMP&nbsp;T,R8   | Set F to flags reflecting the result of T register minus 8 bit register | |
| CMP&nbsp;R8,i8  | Set F to flags reflecting the result of 8 bit register minus immediate byte | |
| CMP&nbsp;FT,R16 | Set F to flags reflecting the result of FT register minus 16 bit register | |
| TST&nbsp;R16    | Set F to flags reflecting the result of 16 bit register minus zero | |

## Program flow
| Mnemonic       | Operation | Remarks |
|----------------|-----------|---------|
| DJ&nbsp;R8,s8  | Decrement 8 bit register, if result is non-zero jump to PC + s8 ||
| J&nbsp;s8      | Jump to PC + s8 ||
| J&nbsp;(r16)   | Jump to address in 16 bit register ||
| JAL&nbsp;(r16) | Jump to address in 16 bit register and store return address in HL ||
| JAL&nbsp;i16   | Jump to address in and store return address in HL |(synthesized)|
| J/cc&nbsp;s8   | If register F satisfies the condition, jump to PC + s8 | |
| RETI           | Return from an interrupt service routine ||
| SYS&nbsp;i8    | Perform a system call to vector i8 ||

## Register transfer
| Mnemonic        | Operation | Remarks |
|-----------------|-----------|---------|
| EXG&nbsp;T,F    | Exchange contents of T with F ||
| EXG&nbsp;FT,R16 | Exchange contents of FT with 16 bit register ||
| LD&nbsp;R8,i8   | Load 8 bit register with immediate byte ||
| LD&nbsp;T,R8    | Load T register with contents of 8 bit register ||
| LD&nbsp;R8,T    | Load 8 bit register with contents of T ||
| LD&nbsp;FT,R16  | Load FT register with contents of 16 bit register ||
| LD&nbsp;R16,FT  | Load 16 bit register with contents of FT ||
| LD&nbsp;R16,i16 | Load 16 bit register with immediate 16 bit value |(synthesized)|
| LD&nbsp;T,(R16+) | Load T from data at R16, then increment R16 | native over BC/DE/HL; the FT cell ($14) is illegal |
| LD&nbsp;(R16+),T | Store T to data at R16, then increment R16 | native over BC/DE/HL; the FT cell ($18) is illegal |
| LD&nbsp;F,(R16+) | Load F from data at R16, then increment R16 | native over BC/DE/HL |
| LD&nbsp;(R16+),F | Store F to data at R16, then increment R16 | native over BC/DE/HL |
| POP&nbsp;R16    | Pop contents of 16 bit register stack ||
| POP&nbsp;Rlist  | Pop contents of specified 16 bit register stacks | (synthesized) |
| POPA            | Pop contents of all 16 bit registers stacks ||
| PUSH&nbsp;R16   | Push contents of 16 bit register onto its stack ||
| PUSH&nbsp;Rlist | Push contents of specified 16 bit registers onto their stacks | (synthesized) |
| PUSHA           | Push contents of 16 bit registers onto their stacks ||
| SWAP&nbsp;R16   | Swap two topmost register stack entries ||
| SWAP&nbsp;Rlist | Swap two topmost stack entries for specified registers | (synthesized) |
| SWAPA           | Swap two topmost stack entries for all registers ||
| PICK&nbsp;R16,i8 | Replace register with stack entry at immediate index ||
| STOW&nbsp;R16,i8 | Store register into its stack at immediate index ||

# External Memory and I/O
| Mnemonic          | Operation | Remarks |
|-------------------|-----------|---------|
| LCO&nbsp;T,(R16+) | Load T with byte in code area pointed to by 16 bit register, then increment the register | `$0D–$0F`; `LCO T,(FT)` at `$0C` does not step — the byte is a half of the pointer it would step; the plain `LCO T,(R16)` spelling is synthesized as the stepped load plus `SUB R16,1` |
| LD&nbsp;T,(R16)   | Load T with byte in data area pointed to by 16 bit register |(synthesized) — through BC/DE/HL it is the stepped load plus `ADD R16,-1`; `$01–$07` were retired 2026-11-12, and through FT it is `LD r8,(FT+)` with a `SUB FT,1` rewind |
| LD&nbsp;(R16),T   | Store T in data area pointed to by 16 bit register |(synthesized) — stepped store plus `ADD R16,-1`; no native plain store through a pair |
| LD&nbsp;R8,(FT+)  | Load 8 bit register with byte in data area pointed to by FT, then increment FT | `$2A–$2F`; the hub bytes cannot step — see `LD F/T,(FT)` below |
| LD&nbsp;(FT+),R8  | Store 8 bit register in data area pointed to by FT, then increment FT | `$22–$27`; the plain `LD (FT),R8` / `LD R8,(FT)` spellings are synthesized as the stepped form plus `SUB FT,1` |
| LD&nbsp;F,(FT)    | Load F with byte in data area pointed to by FT | `$28`, no step — the byte is a half of the pointer it would step; was at `$08`/`$09` in the interim of 2026-11-12, back home since 2026-11-13 |
| LD&nbsp;T,(FT)    | Load T with byte in data area pointed to by FT | `$29`, no step; was `$09` (and `$04` before that) |
| LCR&nbsp;T,(C)    | Load T with contents of configuration register ||
| LCR&nbsp;(C),T    | Load configuration register with contents of T ||
| LIO&nbsp;T,(R16+) | Load T with byte in I/O space pointed to by 16 bit register, then increment the register | `$35–$37`; `LIO T,(FT)` at `$34` does not step; the plain `LIO T,(R16)` spelling is synthesized as the stepped load plus `SUB R16,1` |
| LIO&nbsp;(R16+),T | Store T in I/O space pointed to by 16 bit register, then increment the register | `$31–$33`; `$30` is `NOT F`, so there is no FT store cell; the plain spelling synthesizes with a `SUB R16,1` rewind |

## System control
| Mnemonic | Operation | Remarks |
|----------|-----------|---------|
| DI | Disable external interrupts | `$61` since 2026-11-12, under `RETI`; was `$09` |
| EI | Enable external interrupts | `$69` since 2026-11-12, under `DI`; was `$08` |
| NOP | No operation ||

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)
