// Load the prigram and set up;
load Max.asm,
output-file Max.out,
compare-to Max.cmp,
output-list RAM[0] %D2.6.2
            RAM[1] %D2.6.2
            RAM[2] %D2.6.2;
// Test 1: max(15, 32)
set RAM[0] 15;
set RAM[1] 32;
repeat 14 {
  ticktock;
}
output;     // to the Max.out file

// Test 2: max(47, 22)
set RAM[0] 47;
set RAM[1] 22;
repeat 14 {
  ticktock;
}
output;     // to the Max.out file
