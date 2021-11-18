.section .data
output:
    .ascii "Hello world!\n"
.section .text
.globl main
main:
    movl $4, %eax       # No.4 syscall: write
    movl $1, %ebx       # write to stdout
    movl $output, %ecx  # address of string
    movl $14, %edx      # length of string
    int $0x80           # invoke the syscall
    movl $1, %eax       # No.1 syscall: exit
    movl $0, %ebx       # return code: 0 (success)
    int $0x80

