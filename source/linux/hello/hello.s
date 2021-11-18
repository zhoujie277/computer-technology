	.section	__TEXT,__text,regular,pure_instructions
	.build_version macos, 11, 0	sdk_version 12, 0
	.globl	_sub                            ## -- Begin function sub
	.p2align	4, 0x90
_sub:                                   ## @sub
	.cfi_startproc
## %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	movl	%edi, -4(%rbp)
	movl	$10, -8(%rbp)
	movl	$5, -12(%rbp)
	movl	$20, -16(%rbp)
	movl	$32, -20(%rbp)
	movl	-8(%rbp), %eax
	subl	-12(%rbp), %eax
	addl	-4(%rbp), %eax
	addl	-20(%rbp), %eax
	subl	-16(%rbp), %eax
	popq	%rbp
	retq
	.cfi_endproc
                                        ## -- End function
	.globl	_main                           ## -- Begin function main
	.p2align	4, 0x90
_main:                                  ## @main
	.cfi_startproc
## %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	subq	$16, %rsp
	movl	$0, -4(%rbp)
	movl	$2, %edi
	callq	_sub
	movl	%eax, -8(%rbp)
	movl	-8(%rbp), %esi
	leaq	L_.str(%rip), %rdi
	movb	$0, %al
	callq	_printf
	movl	$1, %eax
	addq	$16, %rsp
	popq	%rbp
	retq
	.cfi_endproc
                                        ## -- End function
	.section	__TEXT,__cstring,cstring_literals
L_.str:                                 ## @.str
	.asciz	"hello, linux, %d"

.subsections_via_symbols
