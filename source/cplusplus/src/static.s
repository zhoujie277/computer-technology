	.section	__TEXT,__text,regular,pure_instructions
	.build_version macos, 11, 0	sdk_version 12, 0
	.globl	_test                           ## -- Begin function test
	.p2align	4, 0x90
_test:                                  ## @test
	.cfi_startproc
## %bb.0:
	pushq	%rbp
	.cfi_def_cfa_offset 16
	.cfi_offset %rbp, -16
	movq	%rsp, %rbp
	.cfi_def_cfa_register %rbp
	movq	_g@GOTPCREL(%rip), %rax
	movb	$97, 4(%rax)
	movl	(%rax), %ecx
	movl	%ecx, -4(%rbp)
	movl	(%rax), %eax
	movl	%eax, -8(%rbp)
	movl	-4(%rbp), %eax
	movl	%eax, -12(%rbp)
	popq	%rbp
	retq
	.cfi_endproc
                                        ## -- End function
	.comm	_g,8,2                          ## @g
.subsections_via_symbols
