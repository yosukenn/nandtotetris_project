// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Mult.asm

// Multiplies R0 and R1 and stores the result in R2.
// (R0, R1, R2 refer to RAM[0], RAM[1], and RAM[2], respectively.)

// Put your code here.
    @0
    D=M
    @i
    M=D
    @sum
    M=0       // sum=0
    @1
    D=M       // D=RAM[1]
    @count
    M=D       // count=RAM[1]
  (LOOP)
    @count
    D=M
    @END
    D;JEQ     // if (count = 0) goto END
    @i
    D=M
    @sum
    M=D+M     // sum += RAM[0]
    @count
    M=M-1
    @LOOP
    0;JMP
  (END)
    @sum
    D=M
    @2
    M=D
    @END
    0;JMP
