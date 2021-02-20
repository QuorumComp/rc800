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

## Synthesized instructions
For improved quality of life, the assembler provides several synthesized instructions. This includes instructions such as ```LD FT,Label``` which would be cumbersome to write manually every time the programmer needed to load the address of a label. These instructions are marked as ```(synthesized)``` in the tables below.

All mnemonics may be followed by a condition code. The assembler will synthesize this into a ```J/CC``` (with the opposite condition CC) and the instruction.

For instance ```LD/EQ T,1``` will load T with the value 1 if the flags in F satisfy the EQ condition.


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
| EXG&nbsp;T,R8   | Exchange contents of T with 8 bit register ||
| EXG&nbsp;FT,R16 | Exchange contents of FT with 16 bit register ||
| LD&nbsp;R8,i8   | Load 8 bit register with immediate byte ||
| LD&nbsp;T,R8    | Load T register with contents of 8 bit register ||
| LD&nbsp;R8,T    | Load 8 bit register with contents of T ||
| LD&nbsp;FT,R16  | Load FT register with contents of 16 bit register ||
| LD&nbsp;R16,FT  | Load 16 bit register with contents of FT ||
| POP&nbsp;R16    | Pop contents of 16 bit register stack ||
| POP&nbsp;Rlist  | Pop contents of specified 16 bit register stacks | (synthesized) |
| POPA            | Pop contents of all 16 bit registers stacks ||
| PUSH&nbsp;R16   | Push contents of 16 bit register onto its stack ||
| PUSH&nbsp;Rlist | Push contents of specified 16 bit registers onto their stacks | (synthesized) |
| PUSHA           | Push contents of 16 bit registers onto their stacks ||
| SWAP&nbsp;R16   | Swap two topmost register stack entries ||
| SWAP&nbsp;Rlist | Swap two topmost stack entries for specified registers | (synthesized) |
| SWAPA           | Swap two topmost stack entries for all registers ||

# External Memory and I/O
| Mnemonic          | Operation | Remarks |
|-------------------|-----------|---------|
| LCO&nbsp;T,(R16)  | Load T with byte in code area pointed to by 16 bit register ||
| LD&nbsp;T,(R16)   | Load T with byte in data area pointed to by 16 bit register ||
| LD&nbsp;(R16),T   | Store T in data area pointed to by 16 bit register ||
| LD&nbsp;R8,(FT)   | Load 8 bit register with byte in data area pointed to by FT ||
| LD&nbsp;(FT),R8   | Store 8 bit register in data area pointed to by FT ||
| LCR&nbsp;T,(C)    | Load T with contents of configuration register ||
| LCR&nbsp;(C),T    | Load configuration register with contents of T ||
| LIO&nbsp;T,(R16)  | Load T with byte in I/O space pointed to by 16 bit register ||
| LIO&nbsp;(R16),T  | Store T in I/O space pointed to by 16 bit register ||

## System control
| Mnemonic | Operation | Remarks |
|----------|-----------|---------|
| DI | Disable external interrupts ||
| EI | Enable external interrupts ||
| NOP | No operation ||

# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)
