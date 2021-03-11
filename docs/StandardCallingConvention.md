## Register preservation
A function must preserve all registers except registers used for return values.

A function should prefer FT for return values, but may use other or additional registers.

If a return value is stored in an eight bit register, the register pair's other register may be used as a temporary register, and the caller should assumed it is not preserved.
