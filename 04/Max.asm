// Computes M[2]=max(M[0], M[1]) where M stands for RAM
    @0
    D=M        // D=M[0]
    @1
    D=D-M
    @FIRST_IS_GREATER
    D;JGT      // If D>0 goto FIRST_IS_GREATER
    @1
    D=M        // D=M[1]
    @SECOND_IS_GREATER
    0;JMP
  (FIRST_IS_GREATER)
    @0
    D=M        // D=first number
  (SECOND_IS_GREATER)
    @2
    M=D        // M[2]=D (greater number)
  (INFINITE_LOOP)
    @INFINITE_LOOP
    0;JMP
