// This file is part of www.nand2tetris.org
// and the book "The Elements of Computing Systems"
// by Nisan and Schocken, MIT Press.
// File name: projects/04/Fill.asm

// Runs an infinite loop that listens to the keyboard input.
// When a key is pressed (any key), the program blackens the screen,
// i.e. writes "black" in every pixel;
// the screen should remain fully black as long as the key is pressed.
// When no key is pressed, the program clears the screen, i.e. writes
// "white" in every pixel;
// the screen should remain fully clear as long as no key is pressed.

// Put your code here.
  (LOOP)
    @23296
    D=A             // D=23296
    @endofscreen
    M=D             // RAM[endofscreen] = 23496
    @SCREEN
    D=A             // D=16384
    @screencount    // RAM[screencount] = 16384
    M=D
    @KBD
    D=M
    @BLACK_SCREEN
    D;JGT
    @WHITE_SCREEN
    D;JLE
    @LOOP
    0;JMP
  (BLACK_SCREEN)
    @screencount
    D=M
    A=D
    M=-1
    D=D+1
    @screencount
    M=D
    @endofscreen
    D=D-M
    @BLACK_SCREEN
    D;JLE
    @LOOP
    0;JMP
  (WHITE_SCREEN)
    @screencount
    D=M
    A=D
    M=0
    D=D+1
    @screencount
    M=D
    @endofscreen
    D=D-M
    @WHITE_SCREEN
    D;JLE
    @LOOP
    0;JMP
