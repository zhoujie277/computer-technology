;演示中断和平坦模式的mbr引导程序

core_base_address equ 0x00040000    ;常数 内核加载的起始物理内存地址
core_start_sector equ 0x00000001    ;常数 内核起始逻辑扇区号

section mbr vstart=0x00007c00

        mov ax, cs
        mov ss, ax
        mov sp, 0x7c00

        ;计算在GDT所在的逻辑段地址
        mov eax, [cs:pgdt + 0x02]       ;GDT的32位物理地址
        xor edx, edx
        mov ebx, 16
        div ebx                         ;分解成16位逻辑地址
        mov ds, eax                     ;ds指向该段以进行操作
        mov ebx, edx                    ;段内其实偏移地址

        ;跳过#0号描述符的槽位
        ;创建1#描述符，保护模式下的代码段描述符
        mov dword [ebx + 0x08], 0x0000ffff  ;基地址为0，界限0xfffff，dpl=00
        mov dword [ebx + 0x0c], 0x00cf9800  ;4KB粒度，代码段描述符，向上扩展

        ;创建2#描述符，保护模式下的数据段和栈段描述符
        mov dword [ebx + 0x10], 0x0000ffff  ;基地址0，界限0xfffff，dpl=00
        mov dword [ebx + 0x14], 0x00cf9200  ;4KB粒度，数据段描述符，向上扩展

        ;初始化靠舒服表寄存器
        mov word [cs:pgdt], 23      ;描述符表的界限
        lgdt [cs:pgdt]

        in al, 0x92
        or al, 0000_0010B
        out 0x92, al        ;打开A20地址线

        cli

        mov eax, cr0
        or eax, 1
        mov cr0, eax        ;设置PE位

        ;以下进入保护模式
        jmp dword 0x0008:flush

        [bits 32]
    flush:
        mov eax, 0x0010             ;加载数据段选择子
        mov ds, eax
        mov es, eax
        mov fs, eax
        mov gs, eax
        mov ss, eax                 ;加载栈段选择子
        mov esp, 0x7000             ;栈指针

        ;以下加载系统核心程序
        mov edi, core_base_address

        mov eax, core_start_sector
        mov ebx, edi                ;起始地址
        call read_hard_disk_0       ;以下读取程序的起始部分（一个扇区）

        ;以下判断整个程序有多大
        mov eax, [edi]              ;核心程序尺寸
        xor edx, edx
        mov ecx, 512                ;512字节每扇区
        div ecx

        or edx, edx
        jnz @1                      ;未除尽，因此结果比实际扇区数少1
        dec eax                     ;已经读了一个扇区，扇区总数减1
    @1:
        or eax, eax                 ;考虑实际长度≤512个字节的情况
        jz pge                      ;EAX=0 ?
        ;读取剩余的扇区
        mov ecx, eax                ;32位模式下的Loop使用ecx
        mov eax, core_start_sector 
        inc eax                     ;从下一个逻辑扇区接着读
    @2:
        call read_hard_disk_0
        inc eax
        loop @2                     ;循环读，知道读完整个内核
    pge:
        ;准备打开分页机制
        ;创建系统内核的页目录表PDT
        mov ebx, 0x00020000         ;页目录表PDT的物理地址

        ;在页目录内创建指向页目录表自己的目录项
        mov dword [ebx + 4092], 0x00020003

        mov edx, 0x00021003
        ;在页目录内创建于线性地址0x00000000对应的目录项
        mov [ebx + 0x000], edx      ;写入目录项（页表的物理地址和属性），此目录项仅用于过度
        ;在页目录内创建与线性地址0x80000000对应的目录项
        mov [ebx + 0x800], edx
        ;创建与上面那个目录项相对应的页表，初始化页表项
        mov ebx, 0x00021000         ;页表的物理地址
        xor eax, eax
        xor esi, esi
    .b1:
        mov edx, eax
        or edx, 0x00000003
        mov [ebx + esi * 4], edx        ;登记页的物理地址
        add eax, 0x1000                 ;下一个相邻页的物理地址
        inc esi
        cmp esi, 256                    ;仅低端1MB内存对应的页才是有效的
        jl .b1
        
        ;令CR3寄存器指向页目录
        mov eax, 0x00020000             ;PCD=PWT=0
        mov cr3, eax

        ;将GDT的线性地址映射到从0x80000000开始的相同位置
        sgdt [pgdt]
        mov ebx, [pgdt + 2]
        add dword [pgdt + 2], 0x80000000    ;GDTR也用的是线性地址
        lgdt [pgdt]

        ;正式开启叶功能
        mov eax, cr0
        or eax, 0x80000000
        mov cr0, eax

        ;将栈段映射到高端，应当把内核所有东西都移到高端线性地址。否则，一定会和正在加载的用户程序任务局部空间里的内容冲突
        add esp, 0x80000000
        ;跳转到内核程序
        jmp [0x80040004]

;从硬盘读取一个逻辑扇区
;EAX=逻辑扇区号
;DS:EBX=目标缓冲区地址
;返回 EBX=EBX+512
read_hard_disk_0:

        push eax
        push ecx
        push edx

        push eax
        mov dx, 0x1f2
        mov al, 1
        out dx, al

        inc dx
        pop eax
        out dx, al

        inc dx
        mov cl, 8
        shr eax, cl
        out dx, al

        inc dx
        shr eax, cl
        out dx, al

        inc dx
        shr eax, cl
        or al, 0xe0
        out dx, al

        inc dx
        mov al, 0x20
        out dx, al
    .waits:
        in al, dx
        and al, 0x88
        cmp al, 0x08
        jnz .waits
        mov ecx, 256
        mov dx, 0x1f0
    .readw:
        in ax, dx
        mov [ebx], ax
        add ebx, 2
        loop .readw

        pop edx
        pop ecx
        pop eax
        ret

pgdt    dw 0
        dd 0x00008000   ;GDT的起始物理地址
    times 510 - ($ - $$) db 0
                         db 0x55, 0xAA