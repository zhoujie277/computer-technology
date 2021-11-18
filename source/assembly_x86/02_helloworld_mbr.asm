                                                ; 程序功能，往主引导扇区写入并最终在屏幕显示 hello，world!
    jmp near start                              ; 跳转到代码段执行
    string db 'hello, world!'                   ; 定义需要在屏幕上显示的数据：hello,world!
    seg_screen equ 0xB800                       ; equ定义常量，屏幕上显示的内存首地址
    seg_offset equ 0x07c0                       ; equ定义常量，mbr 被加载到内存后的地址。0x7c00处。特定义0x07c0作为段地址方便寻址使用。
start:                                          ; 标号，此处表示代码段开始处的汇编地址
    mov ax, seg_screen                          ; 用常量的方式将屏幕的首地址送入ax寄存器。
    mov es, ax                                  ; 将 ax 寄存器的值 0xB800 送入 es 段寄存器
    mov ax, seg_offset                          ; 用常量的方式将段地址 0x07c0 送入 ax 寄存器
    mov ds, ax                                  ; 将 ax 寄存器的值 0x07c0 送入 ds 段寄存器
    mov si, 0                                   ; 将 si 寄存器的值置为 0
    xor di, di                                  ; 将 di 寄存器的值置为 0
    mov cx, start - string                      ; 将代码标号start的汇编地址 - 数据标号string的值送入cx寄存器作为循环计数使用
print:                                          ; 标号，表示 print 函数
    mov al, [si + string]                       ; 将 ds:[si + string] 也就是将 ‘hello, world!'中的字节逐个送入 al 寄存器
    mov es:[di], al                             ; 将 al 寄存器中的内容送入 屏幕显示字的高字节的内存地址: es:[di]
    inc di                                      ; di 寄存器的值自增
    mov byte es:[di], 0x07                      ; 将 0x07 黑底白字送入屏幕显示字的内存地址的低字节，表示需要展示的颜色。
    inc di                                      ; di 寄存器的值自增
    inc si                                      ; si 寄存器的值自增
    loop print                                  ; cx 寄存器的计数减一，不为零则继续循环执行 print
infi:                                           ; 标号 infi
    jmp near infi                               ; 无限循环，跳转到自身这一行
    times (510 - $ + $$) db 0                   ; 填充主引导记录的512个字节
    db 0x55, 0xAA                               ; 主引导记录的有效标识位，最后两个字节必须为 0x55，0xAA
