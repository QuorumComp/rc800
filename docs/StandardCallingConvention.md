# Standard Calling Convention

This is the standard calling convention for the RC800 family. Other calling conventions are possible, this is the officially suggested one.

# Notation

As each machine register is a stack, and parameters may be passed on these stacks, stack values are represented by adding apostrophes to the register mnemonic.

`FT` refers to the current (topmost) value of the `FT` register.

`FT'` is the next value on the stack, the one that will appear after a `POP FT` instruction.

`FT''` is the next value again.

and so forth.

`R` may refer to any 16 bit register.

`multi-word` refers to a value or object larger than 16 bits.

# Basic passing of parameters to subroutines

Eight bit parameters can be passed in the first available 8 bit register, starting from `T` and then `B`, `C`, `D` and `E`. `HL` is not used as this is will hold the return address. `F` is not used to pass an 8 bit value.

Sixteen bit parameters can be passed in the first available 16 bit register, starting from `FT` and then `BC` and `DE`. `HL` is not used as this will hold the return address.

# Returning values

Values are returned in the FT register or, in the case of multi-word values, on the FT stack.

# Passing multi-word values

Parameters 32 bits or larger can be passed on one register stack. In the case of 32 bit integers, `R` and `R'` hold the two 16 bit words, the most significant word in `R`.

# Preserved registers

The convention is generally "callee-saves". All values, including the ones held on the stack, must be preserved by the callee for registers `BC`, `DE` and `HL`.

The register `FT` is "caller-saves". `FT` must be assumed to be destroyed by the callee, even if the callee does not return a value.

If a multi-word parameter is passed on the `FT` stack, the callee will consume this value and remove it from the stack. Note that "consuming" a value means removing one less word than the value actually uses. In the case of a 32 bit value, it takes up one extra word of stack space, thus only one word should be removed by the callee.


# Continue reading
[Introduction and overview](Introduction.md)

[Instruction groups](InstructionGroups.md)

[Opcode matrix](OpcodeMatrix.md)

[Alphabetical list of mnemonics](AlphabeticalMnemonics.md)

[Configuration registers](ConfigurationRegisters.md)

[Interrupts](Interrupts.md)

[Calling convention](StandardCallingConvention.md)
