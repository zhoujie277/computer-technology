; 该程序较有加载器的程序加载，模拟操作系统中一个用户程序被加载的过程
; + 加载器将程序加载到某个内存地址处
; + 加载器在加载用户程序前，将es和ds的段地址都指向了用户程序被加载到内存中的位置
; + 用户头部定义了符合加载器加载格式的程序头部协议。
; + 该程序头部2个字节定义程序的大小，紧接着两个字节表示程序入口点的偏移地址，程序被编译后，该处目前是汇编地址
; + 紧接着4个字节表示代码段的段地址，编译成机器码之后，该处写入的是汇编地址。
; + 紧接着是段重定位表项，表项个数没限制。
; + 当该程序被加载到内存运行之后，加载器会做三件事。
;   1. 重写 begin 处的4个字节表示 段地址:偏移地址,
;   2. 加载器会重写该处的段地址，使之表示为当时运行环境真实的物理段地址。
;   3. 段重定位表和上述逻辑一样。
; + 用户程序被加载后，用户程序首先要做的事就是将段寄存器cs、ds、ss指向自己程序内部的段地址

section header vstart=0                     ;定义用户程序头部段
    program_length dd program_end           ;程序总长度[0x00]
    ;用户程序入口点
    code_entry     dw begin                 ;偏移地址[0x04]
                   dd section.code.start    ;段地址[0x06]

    ; 段重定位表项个数[0x0a]
    realloc_tbl_len dw (header_end - code_segment) / 4

    code_segment dd section.code.start      ;[0x0c]
    data_segment dd section.data.start      ;[0x10]
    stack_segment dd section.stack.start    ;[0x14]
    header_end:
    
section code align=16 vstart=0
; 函数功能
; 将数据段的数据‘hello,world‘输出到屏幕上
; 通过 ds:[si] 指向数据段数据
; 通过 es:[di] 输出屏幕
print:
    push es
    push ax
    push bx
    push si
    push di

    mov ax, [screen_out]
    mov es, ax
    mov si, content
    xor di, di
    .next:  
        mov al, [si]
        cmp al, 0
        jz break
        mov es:[di], al
        inc di
        mov byte es:[di], 0x07
        inc di
        inc si
        jmp short .next
    break:
    pop di
    pop si
    pop bx
    pop ax
    pop es
    ret

begin:
    ; 尚未修改 ds 前，ds 指向的是本程序在内存中加载的地址。
    ; 修改数据段、栈段地址为本程序的段地址
    mov ax, [stack_segment]
    mov ss, ax
    mov sp, stack_end

    mov ax, [data_segment]
    mov ds, ax

    call print

    jmp $

section data align=16 vstart=0
    screen_out dw 0xB800
    content db 'hello, world!',0

section stack align=16 vstart=0
    resb 256
stack_end:

section trail align=16
program_end: