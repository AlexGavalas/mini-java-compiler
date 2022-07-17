@.Factorial_vtable = global [0 x i8] []
@.Fac_vtable = global [1 x i8*] [i8* bitcast (i32 (i8*,i32)* @Fac.ComputeFac to i8*)]

declare i8* @calloc(i32, i32)
declare i32 @printf(i8*, ...)
declare void @exit(i32)@_cint = constant [4 x i8] c"%d\0a\00"
@_cOOB = constant [15 x i8] c"Out of bounds\0a\00"

define void @print_int(i32 %i) {
    %_str = bitcast [4 x i8]* @_cint to i8*
    call i32 (i8*, ...) @printf(i8* %_str, i32 %i)
    ret void
}

define void @throw_oob() {
    %_str = bitcast [15 x i8]* @_cOOB to i8*
    call i32 (i8*, ...) @printf(i8* %_str)
    call void @exit(i32 1)
    ret void
}

define i32 @main() {
%_0 = load i32, i32* %
call void (i32) @print_int(i32 %_0)
ret i32 0
}
define i32 @Fac.ComputeFac(i8* %this, i32 %.num) {
%num = alloca i32
store i32 %.num, i32* %num
) {
%num_aux = alloca i32
%_2 = alloca i32
store i32 1, i32* %_2
%_3 = load i32 , i32* %num
%_4 = load i32 , i32* %_2
%_5 = icmp slt i32 %_3, %_4
%_1 = alloca i1
store i1 %_5 , i1* %_1
%_6 = load i1, i1* %_1
br i1 %_6, label %label0, label %label1
label0:
%_7 = alloca i32
store i32 1, i32* %_7
%_8 = load i32, i32* %_7
store i32 %_8 , i32* %num_aux
br label %label2
label1:
%_17 = load i8*, i8** %this
%_11 = bitcast i8* %_17 to i8***
%_12 = load i8**, i8*** %_11
%_13 = getelementptr i8*, i8** %_12, i32 0
%_14 = load i8*, i8** %_13
%_15 = bitcast i8* %_14 to i32(i8*, i32)*
%_19 = alloca i32
store i32 1, i32* %_19
%_20 = load i32 , i32* %num
%_21 = load i32 , i32* %_19
%_22 = sub i32 %_20, %_21
%_18 = alloca i32
store i32 %_22 , i32* %_18
%_16 = call i32 %_15(i8* %this, i32 %_18)
%_10 = alloca i32
store i32 %_16, i32* %_10
%_23 = load i32 , i32* %num
%_24 = load i32 , i32* %_10
%_25 = mul i32 %_23, %_24
%_9 = alloca i32
store i32 %_25 , i32* %_9
%_26 = load i32, i32* %_9
store i32 %_26 , i32* %num_aux
br label %label2
label2:
%_27 = load i32, i32* num_aux
ret i32 %_27
}
