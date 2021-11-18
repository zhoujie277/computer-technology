;模拟加载器的主引导程序

app_lba_start equ 100

section mbr align=16 vstart=0x7c00

    ; 设置堆栈段和栈指针
    mov ax, 0
    mov ss, ax
    mov sp, ax

    mov ax, [cs:phy_base]               ;计算用于加载用户程序的逻辑段地址
    mov dx, [cs:phy_base + 0x02]
    mov bx, 16
    div bx
    mov ds, ax                          ;令DS和ES指向该段以进行操作
    mov es, ax

    ; 以下读取程序的起始部分
    xor di, di
    mov si, app_lba_start               ;程序在硬盘的起始逻辑扇区号
    xor bx, bx
    call read_hard_disk_0

    ; 以下判断整个程序有多大
    mov dx, [2]
    mov ax, [0]
    mov bx, 512                         ;512字节每扇区
    div bx
    cmp dx, 0
    jnz @1                              ;未除尽，因此结果比实际扇区数少1
    dec ax                              ;已经读了一个扇区，扇区总数减1
@1:
    cmp ax, 0                           ;考虑实际长度小于等于512个字节的情况
    jz direct

    ;读取剩余的扇区
    push ds                             ;以下要用到并改变DS寄存器

    mov cx, ax
@2:
    mov ax, ds
    add ax, 0x20                        ;得到下一个以512字节为边界的段地址
    mov ds, ax

    xor bx, bx                          ;每次读时，偏移地址始终为0x0000
    inc si                              ;下一个逻辑扇区
    call read_hard_disk_0
    loop @2

    pop ds

    ; 计算入口点段基址
direct:
    mov dx, [0x08]
    mov ax, [0x06]
    call calc_segment_base
    mov [0x06], ax                      ;回填修正后的入口点代码段基址

    ; 开始处理段重定位表
    mov cx, [0x0a]                      ; 需要重定位的项目数量
    mov bx, 0x0c                        ; 重定位表首地址

realloc:
    mov dx, [bx + 0x02]                 ; 32位地址的高16位
    mov ax, [bx]
    call calc_segment_base
    mov [bx], ax                        ;回填段地址
    add bx, 4                           ;下一个重定位项
    loop realloc
    ;转移到用户程序, 物理地址为 [0x10000 + 0x04]中的值
    jmp far [0x04]                      

    ;以下从硬盘读取一个逻辑扇区，
    ;输入：DI:SI = 起始逻辑扇区号, DS:BX = 目标缓冲区地址
read_hard_disk_0:                       
    push ax
    push bx
    push cx
    push dx

    ;指示要读取的扇区数
    mov dx, 0x1f2
    mov al, 1
    out dx, al                         

    inc dx                              ;0x1f3
    mov ax, si
    out dx, al                          ;LBA地址0~7

    inc dx                              ;0x1f4
    mov al, ah
    out dx, al                          ;LBA地址8～15

    inc dx                              ;0x1f5
    mov ax, di
    out dx, al                          ;LBA地址16～23

    inc dx                              ;0x1f6
    mov al, 0xe0                        ;LBA28模式，主盘
    or al, ah                           ;LBA地址24～27
    out dx, al

    inc dx                              ;0x1f7
    mov al, 0x20                        ;读命令
    out dx, al

.waits:
    in al, dx                           ;0x1f7既是命令端口，又是状态端口
    and al, 0x88
    cmp al, 0x08
    jnz .waits                          ;不忙，且硬盘已准备好数据传输

    mov cx, 256                         ;读一个扇区
    mov dx, 0x1f0                       ;0x1f0 数据端口
.readw:                                 ;将硬盘数据读到内存，
    in ax, dx
    mov [bx], ax
    add bx, 2
    loop .readw

    pop dx
    pop cx
    pop bx
    pop ax
    ret
    
    ; 根据用户程序的汇编地址(偏移地址）计算实际在机器上的16位段地址
    ; 输入：DX:AX = 32位物理地址
    ; 返回: AX = 16位段基地址
calc_segment_base:
    push dx

    add ax, [cs:phy_base]
    adc dx, [cs:phy_base + 0x02]
    shr ax, 4
    ror dx, 4
    and dx, 0xf000
    or ax, dx
    
    pop dx
    ret

    phy_base dd 0x10000                 ;用户程序被加载的物理起始地址

times 510 - ($ - $$) db 0
                     db 0x55, 0xAA